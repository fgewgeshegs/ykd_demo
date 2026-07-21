# 语音输入：从"只能打字"到"可以说话"

## 问题

```
你发：文字消息 → 机器人正常回复 ✓
你发：语音消息 → 机器人不回复 ✗（被 pollLoop 跳过）
```

语音消息发过去石沉大海，因为 `WechatMessageService` 只处理了 `isText()` 和 `isImage()`，`isVoice()` 根本没分支。

## 目标

```
你按住说话："今天天气怎么样"
机器回："今天北京晴，25°C"  ← 语音进来，文字出去
```

---

## 一、整体流程

```
① 微信发来语音消息
     ↓
② pollLoop: item.isVoice() → 构建 WechatMessage(VOICE)
     ↓
③ MessageRouter: VOICE 分支 → VoiceHandler
     ↓
④ VoiceHandler:
    ├─ 微信有服务端转写？→ 直接用（免费、即时）
    ├─ 没有？→ downloadMedia() → SpeechClient(百炼 SenseVoice) → 文字
    └─ 构造虚拟 WechatMessage(TEXT) → messageRouter.route() 重新走
     ↓
⑤ 正常对话链路（意图分类 + 上下文记忆）→ 文字回复
```

**核心思路**：语音只是**输入的皮**，剥掉之后丢回正常的文本对话链路。生图、上下文、天气查询全部自动复用。

---

## 二、改哪些文件（8 个）

| 文件 | 操作 | 改什么 |
|------|------|--------|
| `wechat/model/WechatMessage.java` | 修改 | +3 字段：`voiceEncryptParam`、`voiceAesKey`、`voicePlaytime` |
| `wechat/service/WechatMessageService.java` | 修改 | pollLoop 加 `isVoice()` 分支 |
| `ai/speech/SpeechProperties.java` | **新增** | 百炼 ASR 配置 |
| `ai/speech/SpeechClient.java` | **新增** | 调百炼 SenseVoice API |
| `ai/speech/SpeechService.java` | **新增** | 封装 ASR 调用 |
| `wechat/handler/VoiceHandler.java` | **新增** | 语音处理管线 |
| `wechat/MessageRouter.java` | 修改 | 注入 VoiceHandler + VOICE 路由 |
| `application.properties.example` | 修改 | 加 `speech.*` 配置 |

**没动：** ChatService、LLMClient、ContextStore、AIChatHandler、VisionHandler、ImageGenerationHandler。语音识别出文字后走现有链路，全部复用。

---

## 三、逐个详解

### 1. WechatMessage — 加三个字段

```java
private String voiceEncryptParam;  // CDN 下载加密参数
private String voiceAesKey;        // Base64 解密密钥
private long voicePlaytime;        // 语音时长（毫秒）
```

跟图片的 `imageEncryptParam`/`imageAesKey` 模式完全一致。语音也是通过微信 CDN 下载的媒体文件，需要加密参数和密钥。

### 2. WechatMessageService — pollLoop 加分支

```java
} else if (item.isVoice() && item.getVoice() != null) {
    VoiceContent voice = item.getVoice();
    wechatMsg.setType(MessageType.VOICE);
    wechatMsg.setVoiceEncryptParam(voice.getEncryptQueryParam());
    wechatMsg.setVoiceAesKey(voice.getAesKey());
    wechatMsg.setVoicePlaytime(voice.getPlaytime());
    // 微信服务端转写（可能为 null）
    wechatMsg.setText(voice.getText() != null ? voice.getText() : "");
    log.info("收到语音消息 | from={} | playtime={}ms", fromUserId, voice.getPlaytime());
}
```

SDK 的 `VoiceContent` 跟 `ImageContent` 结构一样——有 `getEncryptQueryParam()`、`getAesKey()`，外加语音专属的 `getPlaytime()`（时长）、`getText()`（微信转写）。

**额外福利：** `voice.getText()` 是微信服务端已经做的语音转文字。如果它有值，直接白嫖，省掉下载+ASR API 全流程。

### 3. SpeechProperties — 配置类

```java
@Data
@Component
@ConfigurationProperties(prefix = "speech")
public class SpeechProperties {
    private String apiKey;
    private String baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/audio/asr/asr";
    private String model = "sensevoice-v1";
}
```

照抄 `VisionProperties` 的模式。`application.properties` 加：

```properties
speech.api-key=sk-ws-H.EDYELLR...
speech.base-url=https://dashscope.aliyuncs.com/api/v1/services/audio/asr/asr
speech.model=sensevoice-v1
```

api-key 跟 Vision/Image 共用同一个百炼 Key。

### 4. SpeechClient — 调百炼 SenseVoice

```java
public String recognize(byte[] audioBytes) {
    // ① 音频 base64 编码
    String base64 = Base64.getEncoder().encodeToString(audioBytes);

    // ② 构建请求体
    // {"model":"sensevoice-v1","input":{"audio":"base64..."}}

    // ③ POST https://dashscope.aliyuncs.com/api/v1/services/audio/asr/asr
    // Header: Authorization: Bearer {api-key}

    // ④ 解析响应
    // {"output":{"text":"识别结果"}}
}
```

HTTP 调用模式照抄 `LLMClient`：自己 new `HttpClient`、Jackson `ObjectMapper`、30 秒超时。

**为什么用 base64 而不是 multipart**：base64 塞 JSON 最简单，跟 VisionClient 传图片的 `data:image/jpeg;base64,...` 思路一致。

### 5. SpeechService — 薄封装

```java
@Service
public class SpeechService {
    public String recognize(byte[] audioBytes) {
        // 校验 → 调 SpeechClient → null 处理 → 返回文字
    }
}
```

跟 `VisionService` 模式一致——Service 负责降级和日志，Client 只负责调 API。后续要加 SILK→WAV 转码也加在这层。

### 6. VoiceHandler — 核心管线

```java
@Component
public class VoiceHandler implements MessageHandler {

    private final SpeechService speechService;
    private final WechatILinkClient wechatClient;
    private final MessageRouter messageRouter;  // @Lazy 破循环

    @Override
    public String handle(WechatMessage message) {
        if (message.getType() != MessageType.VOICE) return null;

        String text;

        // ── ① 优先用微信服务端转写 ──
        if (message.getText() != null && !message.getText().isEmpty()) {
            text = message.getText();
        }

        // ── ② 否则下载音频 + ASR ──
        if (text == null || text.isEmpty()) {
            byte[] audioBytes = wechatClient.downloadMedia(
                message.getVoiceEncryptParam(), message.getVoiceAesKey());
            text = speechService.recognize(audioBytes);
        }

        // ── ③ 构造虚拟 TEXT，重新走路由 ──
        WechatMessage textMsg = new WechatMessage();
        textMsg.setType(MessageType.TEXT);
        textMsg.setText(text);
        textMsg.setUserId(message.getUserId());
        textMsg.setContextToken(message.getContextToken());

        return messageRouter.route(textMsg);
    }
}
```

**为什么自己不发文字，而是重新走 `messageRouter.route()`**：语音识别出的文字可能是"画一只猫"（触发 ImageGenerationHandler），也可能是"今天天气怎么样"（触发 ChatService）。重新走路由让意图分类正常工作。

**为什么注入 `MessageRouter` 用 `@Lazy`**：`MessageRouter` 持有 `VoiceHandler`，`VoiceHandler` 又持有 `MessageRouter`，形成循环。`@Lazy` 让 Spring 先用代理占位，创建完再注入真实对象。

### 7. MessageRouter — 加语音路由

```java
// 语音消息：下载 → ASR → 以文本形式重新路由
if (message.getType() == MessageType.VOICE) {
    log.info("路由：语音消息 → VoiceHandler | from={}", message.getUserId());
    String reply = voiceHandler.handle(message);
    return fallbackIfEmpty(reply, message);
}
```

放在 IMAGE 和 TEXT 之间，跟 IMAGE 一样跳过意图分类直接路由。

---

## 四、完整数据流（一次语音"今天天气怎么样"）

```
══════════════════════════════════════════════════════════
你按住说话："今天天气怎么样"
══════════════════════════════════════════════════════════

1. 微信发来语音消息
     ↓
2. pollLoop:
   item.isVoice() == true
   voice.getText() → null（微信没转写）
   → wechatMsg.type = VOICE
   → wechatMsg.voiceEncryptParam = "xxx"
   → wechatMsg.voiceAesKey = "yyy"
   → wechatMsg.voicePlaytime = 3200
     ↓
3. MessageRouter.route(wechatMsg)
   → type == VOICE → voiceHandler.handle(message)
     ↓
4. VoiceHandler:
   ① message.getText() → ""（空，微信没转写，走自己 ASR）
   ② wechatClient.downloadMedia("xxx", "yyy") → byte[48320]
   ③ speechService.recognize(bytes)
      → SpeechClient: base64编码 → POST 百炼
        Request: {"model":"sensevoice-v1","input":{"audio":"base64..."}}
        Response: {"output":{"text":"今天天气怎么样"}}
      → "今天天气怎么样"
   ④ 构造 WechatMessage:
      type=TEXT, text="今天天气怎么样", userId="user123"
   ⑤ messageRouter.route(这条TEXT) ← 第二次进入路由
         ↓
5. MessageRouter 第二次路由:
   → type == TEXT
   → intentClassifier.classify("今天天气怎么样") → CHAT
   → chatHandler.handle(message)
     → chatService.chat("user123", "今天天气怎么样")
       → contextStore.getHistory("user123") → [历史...]
       → llmClient.chat("今天天气怎么样", history)
       → "今天北京晴，25°C"
       → contextStore.append("user", ...)
       → contextStore.append("assistant", ...)
     → return "今天北京晴，25°C"
   → reply = "今天北京晴，25°C"
     ↓
6. pollLoop: reply != null → sendTextMessage("今天北京晴，25°C")
     ↓
7. 你看到文字回复 ✓
```

---

## 五、和上下文的联动

VoiceHandler 把文字丢回 MessageRouter → AIChatHandler → ChatService，ContextStore 照常工作：

```
第一轮：语音"我叫小明"  → TEXT → ChatService → contextStore 记下
第二轮：语音"我叫什么"  → TEXT → ChatService → getHistory 拿到 → LLM 答对
```

**不需要额外改动任何东西。** VoiceHandler 甚至不知道 ContextStore 的存在。

---

## 六、⚠️ 潜在问题：音频格式

微信语音是 **SILK 格式**（`getEncodeType() == 6`）。百炼 SenseVoice **不支持 SILK**。

**解决：ffmpeg 转码 SILK → WAV**

```bash
ffmpeg -f silk -i input.silk -ar 16000 -ac 1 output.wav
```

两步走：
1. **第一步先不转码**，直接传试试——百炼 API 可能在服务端自动处理常见格式
2. **如果返回格式错误**，在 `SpeechService` 里加 `ProcessBuilder` 调 ffmpeg 转码

部署需要服务器预装 ffmpeg：`apt install ffmpeg`。

---

## 七、为什么这样设计

**为什么语音重新走 MessageRouter 而不是直接调 ChatService**：语音识别出的文字可能触发生图（"画一只猫"）、天气查询等不同意图。重新走路由让 `IntentClassifier` 正常工作。

**为什么用 `@Lazy` 而不是 `allow-circular-references`**：全局开循环引用太粗暴，会影响所有 Bean。`@Lazy` 只在 VoiceHandler 这一个注入点延迟，精确可控。

**为什么先检查微信转写**：免费的、即时的、不需要下载音频。虽然不一定有值，但有值的时候省一次完整的 ASR 流程。

**为什么 SpeechProperties 放独立包 `ai/speech/` 而不是 `ai/llm/`**：语音识别不是 LLM——它不遵循 OpenAI 协议，调用方式不同。独立包以后加 TTS（文字转语音）也可以往里放。
