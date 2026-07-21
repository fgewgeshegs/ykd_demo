# 语音输入 (ASR) 实现方案

## 目标

用户给微信机器人发语音消息 → 机器人识别成文字 → 走正常对话流程 → 返回文字回复。

```
用户按住说话："今天天气怎么样"
     ↓
微信发来语音消息
     ↓
下载音频 → ASR识别 → "今天天气怎么样"（文字）
     ↓
MessageRouter.route(这条文字) → 正常对话（带上下文记忆）
     ↓
回复文字："今天北京晴，25°C"
```

---

## 改哪些文件

| 文件 | 操作 | 改什么 |
|------|------|--------|
| `wechat/model/WechatMessage.java` | 修改 | +2 字段：`voiceEncryptParam`、`voiceAesKey` |
| `wechat/service/WechatMessageService.java` | 修改 | pollLoop 加 `isVoice()` 分支 |
| `wechat/handler/VoiceHandler.java` | **新增** | 语音处理管线 |
| `ai/speech/SpeechProperties.java` | **新增** | 百炼 ASR 配置 |
| `ai/speech/SpeechClient.java` | **新增** | 调百炼 ASR API |
| `ai/speech/SpeechService.java` | **新增** | 封装 ASR 调用 |
| `wechat/MessageRouter.java` | 修改 | 加 VoiceHandler 路由 |

**没动：** ChatService、LLMClient、ContextStore、VisionHandler、ImageGenerationHandler、IntentClassifier。语音识别出文字后走现有链路，上下文自动生效。

---

## 一、微信侧：接收语音消息

### 1.1 WechatMessage — 加字段

现有图片字段已改名为 `imageEncryptParam` / `imageAesKey`，语音加对应的：

```java
// 语音消息的 CDN 下载参数（VOICE 类型时有效）
private String voiceEncryptParam;
private String voiceAesKey;
private int voicePlaytime;     // 语音时长（毫秒）
```

这样图片和语音字段命名一致：`xxxEncryptParam` / `xxxAesKey`。

SDK 已确认：`item.isVoice()` / `item.getVoice()` 返回 `VoiceContent`，包含：

| VoiceContent 字段 | 类型 | 说明 |
|---|---|---|
| `getEncryptQueryParam()` | String | CDN 下载加密参数 |
| `getAesKey()` | String | Base64 解密密钥 |
| `getPlaytime()` | int | 时长（毫秒） |
| `getEncodeType()` | int | 编码类型，6=SILK |
| `getSampleRate()` | int | 采样率 |
| `getText()` | String | 微信服务端转写（可能为 null） |

跟图片的 `ImageContent` 模式完全一致——有加密参数和 AES Key，通过 `downloadMedia()` 下载。

**额外福利**：`getText()` 是微信服务端已经做的语音转文字。如果它不为空，可以直接用，省掉一次 ASR API 调用。但它不一定可靠（可能为 null），所以只做**优先降级**，不替代自己的 ASR。

### 1.2 WechatMessageService — pollLoop 加分支

在 `item.isImage()` 分支后面加：

```java
} else if (item.isVoice() && item.getVoice() != null) {
    VoiceContent voice = item.getVoice();
    wechatMsg.setType(MessageType.VOICE);
    wechatMsg.setVoiceEncryptParam(voice.getEncryptQueryParam());
    wechatMsg.setVoiceAesKey(voice.getAesKey());
    wechatMsg.setVoicePlaytime(voice.getPlaytime());
    // 微信可能已做了转写，作为降级文本
    wechatMsg.setText(voice.getText() != null ? voice.getText() : "");
    log.info("收到语音消息 | from={} | playtime={}ms", fromUserId, voice.getPlaytime());
}
```

### 1.3 不需要加 sendVoiceMessage

语音输入只需要**收**语音，**回**文字。`WechatILinkClient` 不需要加新方法。`sendTextMessage` 已经够用。

---

## 二、AI 侧：百炼 ASR 语音识别

### 2.1 为什么用百炼而不是其他

| 候选 | 理由 |
|------|------|
| **阿里云百炼 SenseVoice**（选这个） | 项目已用百炼（Vision/Image），同一套 AK/Base URL，零额外接入 |
| 阿里云 NLS | 独立语音服务，需单独开通、单独 SDK |
| OpenAI Whisper | 多一层网络，多一个 API Key 管理 |

百炼语音识别 API：
- Endpoint: `https://dashscope.aliyuncs.com/api/v1/services/audio/asr/asr`
- 模型: `sensevoice-v1` (中文识别效果好) 或 `paraformer-v1`
- 输入: 音频文件 bytes（支持 wav/mp3/m4a 等格式）
- 输出: `{output: {text: "识别结果文字"}}`
- 鉴权: `Authorization: Bearer {api-key}`（跟 Vision/Image 一样）

### 2.2 SpeechProperties — 配置类

```java
@ConfigurationProperties(prefix = "speech")
public record SpeechProperties(String apiKey, String baseUrl, String model) {}
```

模仿 `VisionProperties` 的风格（record + `@ConfigurationProperties`）。

`application.properties` 加：

```properties
# Speech Recognition (ASR) Configuration
speech.api-key=sk-ws-H.EDYELLR.09sz...
speech.base-url=https://dashscope.aliyuncs.com/api/v1/services/audio/asr/asr
speech.model=sensevoice-v1
```

api-key 跟 vision/image 共用同一个百炼 Key。

### 2.3 SpeechClient — HTTP 调用

```java
@Component
public class SpeechClient {

    private final SpeechProperties properties;
    private final HttpClient httpClient;     // 每个 Client 自己 new
    private final ObjectMapper objectMapper;

    /**
     * 语音识别
     * @param audioBytes 音频文件字节
     * @return 识别出的文字，失败返回 null
     */
    public String recognize(byte[] audioBytes) {
        // ① 构建 multipart/form-data 请求体
        //    或者用 base64 编码（百炼 ASR 支持 JSON 传 base64）
        // ② POST {baseUrl}
        //    Header: Authorization: Bearer {api-key}
        // ③ 解析 response: output.text
    }
}
```

关键设计点：
- **HTTP 调用模式照抄 `LLMClient`**：自己 new `HttpClient`、Jackson `ObjectMapper`、30 秒超时
- **音频传法**：百炼 ASR 支持两种 —— 文件 URL 或 base64。微信下载到的是 `byte[]`，用 base64 编码塞进 JSON body 最直接，避免多段 form-data 的复杂度
- **错误处理**：调用失败 return null，不抛异常（跟其他 Client 一致）

### 2.4 SpeechService — 封装

```java
@Service
public class SpeechService {

    public String recognize(byte[] audioBytes) {
        // 调 SpeechClient，处理 null/空
        // 可以在这里做：结果清洗、置信度过滤（未来扩展）
    }
}
```

跟 `ChatService`/`VisionService` 模式一致——Service 层负责异常降级、日志、结果处理，Client 层只负责调 API。

---

## 三、VoiceHandler — 核心管线

```java
@Slf4j
@Component
public class VoiceHandler implements MessageHandler {

    private static final String FALLBACK_REPLY = "抱歉，语音识别失败，请发文字试试。";

    private final SpeechService speechService;
    private final WechatILinkClient wechatClient;
    private final MessageRouter messageRouter;

    @Override
    public String handle(WechatMessage message) {
        if (message.getType() != MessageType.VOICE) return null;

        String text = null;

        // ── ① 优先用微信服务端转写（免费，即时） ──
        if (message.getText() != null && !message.getText().isEmpty()) {
            text = message.getText();
            log.info("使用微信服务端转写 | from={} | text={}", message.getUserId(), text);
        }

        // ── ② 微信没转写，自己下载 + ASR ──
        if (text == null || text.isEmpty()) {
            byte[] audioBytes = wechatClient.downloadMedia(
                message.getVoiceEncryptParam(), message.getVoiceAesKey());
            if (audioBytes == null || audioBytes.length == 0) {
                return FALLBACK_REPLY;
            }

            text = speechService.recognize(audioBytes);
            if (text == null || text.isEmpty()) {
                return FALLBACK_REPLY;
            }
            log.info("ASR 识别结果 | from={} | text={}", message.getUserId(), text);
        }

        // ── ③ 构造虚拟文本消息，走正常对话流程 ──
        WechatMessage textMsg = new WechatMessage();
        textMsg.setUserId(message.getUserId());
        textMsg.setType(MessageType.TEXT);
        textMsg.setText(text);
        textMsg.setContextToken(message.getContextToken());

        return messageRouter.route(textMsg);  // 复用路由 → 意图分类 → 上下文
    }
}
```

### 为什么不是自己调 ChatService，而是重新走 MessageRouter

语音识别出的文字可能是：

| 用户说的 | 识别结果 | 应该触发 |
|---------|---------|---------|
| "画一只猫" | "画一只猫" | ImageGenerationHandler（生图） |
| "帮我分析一下这张图" + 发了张图 | "帮我分析一下这张图" | 图片+文字交叉 |
| "今天天气怎么样" | "今天天气怎么样" | ChatService（对话） |

重新走 `MessageRouter.route()` 是为了让**意图分类和路由**正常工作。如果 VoiceHandler 直接调 `ChatService`，那 "画一只猫" 就永远是对话，不会生图。

### 上下文自动生效

VoiceHandler 返回 `MessageRouter.route(textMsg)`，里面走到 `AIChatHandler → ChatService`，ContextStore 的 `getHistory`/`append` 照常工作：

```
第一轮：语音"我叫小明" → 文字"我叫小明" → ChatService → contextStore 记录
第二轮：语音"我叫什么" → 文字"我叫什么" → ChatService → getHistory 拿到之前的记录 → LLM 答对
```

VoiceHandler 不需要知道 ContextStore 的存在。

---

## 四、MessageRouter — 加路由

在 `route()` 方法里加一个语音分支：

```java
// 语音消息：下载 → ASR → 转发文本
if (message.getType() == MessageType.VOICE) {
    log.info("路由：语音消息 → VoiceHandler | from={}", message.getUserId());
    String reply = voiceHandler.handle(message);
    return fallbackIfEmpty(reply, message);
}
```

放在图片消息分支后面即可。

---

## 五、完整数据流

```
══════════════════════════════════════════════════════════
用户按住说话："今天天气怎么样"
══════════════════════════════════════════════════════════

1. 微信发送语音消息
     ↓
2. pollLoop:
   item.isVoice() == true
   → wechatMsg.type = VOICE
   → wechatMsg.voiceEncryptParam = "xxx"
   → wechatMsg.voiceAesKey = "yyy"
     ↓
3. MessageRouter.route(wechatMsg)
   → message.getType() == VOICE
   → voiceHandler.handle(message)
     ↓
4. VoiceHandler:
   ① downloadMedia("xxx", "yyy") → byte[52300] (3秒语音，~50KB)
   ② speechService.recognize(bytes)
      → SpeechClient POST https://dashscope.aliyuncs.com/...
        Body: {"model":"sensevoice-v1","input":{"audio": "base64..."}}
      → Response: {"output":{"text":"今天天气怎么样"}}
      → "今天天气怎么样"
   ③ 构造 WechatMessage(type=TEXT, text="今天天气怎么样")
   ④ messageRouter.route(这条文本消息)
         ↓
   ⑤ MessageRouter 再次路由（这次是 TEXT）
      → intent=CHAT
      → AIChatHandler → ChatService
        → contextStore.getHistory("user123") → [历史消息...]
        → llmClient.chat("今天天气怎么样", history) → "今天北京晴，25°C"
        → contextStore.append(user + assistant)
   ⑥ 返回 "今天北京晴，25°C"
     ↓
5. pollLoop: reply != null → sendTextMessage("今天北京晴，25°C")
     ↓
6. 用户看到文字回复 ✓
```

---

## 六、潜在问题和处理

### 音频格式：SILK

微信语音是 **SILK 格式**（`VoiceContent.getEncodeType() == 6`）。百炼 SenseVoice **不支持 SILK**。

**解法：服务端转码 SILK → WAV**

在 `SpeechService` 里加一步转码。两种方式：

- **方案 A（推荐）**：服务器安装 `ffmpeg`，Java 调命令行：
  ```
  ffmpeg -f silk -i input.silk -ar 16000 -ac 1 output.wav
  ```
- **方案 B**：引入 Java 音频库自己做解码。SILK 是 Skype 的私有格式，Java 生态几乎没有直接支持，不现实。

所以实际上只有 ffmpeg 一条路。部署时服务器需要预装 ffmpeg（`apt install ffmpeg` 或 `brew install ffmpeg`）。代码里用 `ProcessBuilder` 调 ffmpeg，管道输入 bytes，管道输出 WAV bytes。

### 微信服务端转写不一定可靠

微信的 `VoiceContent.getText()` 有时有值有时为 null。实测中应该：

1. 先检查 `getText()` 是否非空 → 非空直接用（省一次下载+ASR+转码）
2. 为空才走下载→转码→ASR 完整流程

这个降级逻辑已经内建在 VoiceHandler 的三步流程里。

### 语音时长

百炼 SenseVoice 单次最长支持 60 秒。微信语音消息通常就几秒到几十秒，足够。如果用户发超长语音，API 会返回错误，SpeechService 捕获后返回降级提示即可。

### 延迟

语音识别增加约 1-3 秒延迟（下载音频 + API 调用）。理想情况下用户感知不到太大差异——发完语音后本来就会等机器人"正在输入..."。

### 成本

百炼 SenseVoice 按音频时长计费，约 0.00008 元/秒。一条 10 秒语音 ≈ 0.0008 元，近乎免费。

---

## 七、和上下文功能的联动

不需要额外做任何事情。VoiceHandler 把语音转成文字后丢回 MessageRouter，文本走 ChatService，ContextStore 照常工作。这是分层设计的好处——加一个新输入模态，不影响对话逻辑。

---

## 八、确认的 iLink SDK API（wechat-ilink-sdk 1.0.1）

### 接收语音消息

```java
// MessageItemDto（pollLoop 用的 DTO）
item.isVoice()          // boolean
item.getVoice()         // → VoiceContent

// VoiceContent 字段
voice.getEncryptQueryParam()  // CDN 下载加密参数
voice.getAesKey()             // Base64 解密密钥
voice.getPlaytime()           // 时长（毫秒）
voice.getEncodeType()         // 编码类型，6=SILK
voice.getSampleRate()         // 采样率
voice.getBitsPerSample()      // 位深度
voice.getText()               // 微信服务端转写（可能为 null）
```

### 发送语音消息（语音输出用，本次改动不需要）

```java
// ILinkClient 已有方法
public void sendVoiceMessage(
    LoginCredentials credentials,
    String toUserId,
    String contextToken,
    MediaInfo mediaInfo,       // 来自 uploadMedia(mediaType=4, ...)
    int duration,              // 时长（毫秒）
    int encodeType             // 6=SILK
)
```

### 媒体操作（已有封装）

```java
// 下载（图片和语音通用，已封装在 WechatILinkClient）
client.downloadMedia(encryptQueryParam, aesKey) → byte[]

// 上传（mediaType: 1=图片, 2=视频, 3=文件, 4=语音，已封装）
client.uploadMedia(credentials, mediaType, toUserId, fileData) → MediaInfo
```

`downloadMedia()` 和 `uploadMedia()` 已在 `WechatILinkClient` 中封装好——语音直接复用，不需要加新方法。
