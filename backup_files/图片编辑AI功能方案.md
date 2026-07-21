# AI 图片编辑功能实现方案

## 目标

用户发一张图 + 文字指令 → AI 按照指令编辑图片 → 返回编辑后的图片。

```
你发：一张猫的照片 + 文字"把背景改成蓝色"
机器回：[编辑后的图片 — 猫不变，背景变蓝色]
```

---

## 关键发现

### SDK：不支持图文混合消息，但可以合并

微信里发"图片+文字"时，SDK 拆成两条独立 Item：一条 IMAGE、一条 TEXT。当前 pollLoop 各自路由，图片走 VisionHandler（分析），文字走 AIChatHandler（对话），互不相干。

**解法：** 在 pollLoop 里检测 itemList 中相邻的 IMAGE+TEXT，合并成一个 WechatMessage（同时带文字和图片数据）。

### 百炼：同一个 API 就支持图生图

当前 `ImageClient` 用的 `qwen-image-2.0` 模型**本身就支持图生图**，同一个 Endpoint、同一个模型、同一个响应格式。唯一的区别是请求体里多加一个 `{"image": "base64或URL"}`。

**不需要换模型、不需要新 API Key、不需要新 Endpoint。** 只在 `buildRequestBody` 里多插一行。

---

## 改哪些文件（9 个）

| 文件 | 操作 | 改什么 |
|------|------|--------|
| `wechat/model/WechatMessage.java` | 修改 | +1 字段 `hasAttachedImage` |
| `wechat/service/WechatMessageService.java` | 修改 | +`mergeImageAndText()` 合并相邻图文 |
| `ai/classifier/Intent.java` | 修改 | +`IMAGE_EDIT` 枚举值 |
| `ai/classifier/LLMIntentClassifier.java` | 修改 | CLASSIFICATION_PROMPT 加编辑意图 |
| `ai/llm/ImageClient.java` | 修改 | +`editImage(prompt, imageDataUrl)` |
| `ai/image/ImageGenerationService.java` | 修改 | +`edit(prompt, imageDataUrl)` |
| `wechat/handler/ImageEditHandler.java` | **新增** | 图片编辑管线 |
| `wechat/MessageRouter.java` | 修改 | +IMAGE_EDIT 路由 + 新增 Handler 注入 |

**没动：** VisionHandler、AIChatHandler、VoiceHandler、ContextStore、ImageGenerationHandler、ImageProperties。

---

## 一、输入处理：合并图文消息

### 问题

用户在微信里选一张图 + 打字"把背景改成蓝色" → SDK 收到两条 Item：

```
itemList[0]: IMAGE (加密参数)
itemList[1]: TEXT "把背景改成蓝色"
```

当前 pollLoop 各自构建 WechatMessage、各自路由，图文被当两条独立消息。

### 解法：pollLoop 合并相邻图文

在 `forEach(item)` 之前，先扫描 itemList，找到相邻的 IMAGE+TEXT 对，合并成一个：

```java
// 在 pollLoop 中，构建 WechatMessage 之前先检查合并
List<MessageItemDto> mergedItems = mergeImageAndText(msg.getItemList());

mergedItems.forEach(item -> {
    WechatMessage wechatMsg = new WechatMessage();
    // ... 现有解析逻辑 ...

    // 新增：如果是合并后的图文消息
    if (isMergedImageText(item)) {
        wechatMsg.setHasAttachedImage(true);
        wechatMsg.setType(MessageType.TEXT);  // 按文字意图走路由
    }
});
```

合并逻辑：

```
itemList: [IMAGE, TEXT "把背景改成蓝色"]
    ↓ mergeImageAndText
mergedItems: [MERGED{image=IMAGE的CDN参数, text="把背景改成蓝色"}]
```

**合并规则：**
- 如果 itemList 中一条 IMAGE 后面紧跟一条 TEXT（或反过来）→ 合并
- 合并后类型为 TEXT（文字表达用户意图，图片作为附件）
- 图片的 CDN 参数填充到 `imageEncryptParam`/`imageAesKey`
- 设置 `hasAttachedImage = true`

### WechatMessage 加标记字段

```java
/** 是否附带图片（图文合并消息时有效） */
private boolean hasAttachedImage;
```

不需要新字段存图片数据——现有的 `imageEncryptParam`/`imageAesKey` 直接复用。

---

## 二、意图分类：加 IMAGE_EDIT

### Intent.java

```java
public enum Intent {
    CHAT,
    IMAGE_GENERATE,   // 文生图
    IMAGE_ANALYZE,    // 图片分析
    IMAGE_EDIT         // 图片编辑 ← 新增
}
```

### LLMIntentClassifier — 改 CLASSIFICATION_PROMPT

在 prompt 中增加编辑意图的描述：

```
- IMAGE_EDIT: 用户想修改、编辑已有图片，例如"把背景改了""去掉水印"
  "加个帽子""换成卡通风格""把颜色调亮"等
```

LLM 根据文字判断用户是要"生成新图"还是"改已有图片"。

---

## 三、ImageClient：加图生图方法

现有 `generateImage(prompt)` → 新增 `editImage(prompt, imageDataUrl)`。

唯一的区别在请求体：

```java
// 现有（文生图）
"content": [{ "text": "一只猫" }]

// 新增（图生图/编辑）
"content": [
    { "image": "data:image/jpeg;base64,..." },  // ← 多这一行
    { "text": "把背景改成蓝色" }
]
```

`imageDataUrl` 的格式跟 VisionClient 一样——`data:image/jpeg;base64,...`。ImageEditHandler 下载图片后 base64 编码即可。

响应解析**完全不变**——`output.choices[0].message.content[0].image`。

```java
/**
 * 编辑图片（图生图）
 *
 * @param prompt       编辑指令，如"把背景改成蓝色"
 * @param imageDataUrl 原图（base64 data URL 或 HTTP URL）
 * @return 编辑后的图片 URL，失败返回 null
 */
public String editImage(String prompt, String imageDataUrl) {
    // buildRequestBody(prompt, imageDataUrl) — 多一个 image content
    // 其余逻辑跟 generateImage() 完全一样
}
```

---

## 四、ImageEditHandler — 核心管线

```java
@Slf4j
@Component
public class ImageEditHandler implements MessageHandler {

    private final ImageGenerationService imageService;
    private final WechatILinkClient wechatClient;
    private final HttpClient httpClient;

    @Override
    public String handle(WechatMessage message) {
        // ── ① 获取原图 ──
        String imageDataUrl;
        if (message.getImageEncryptParam() != null) {
            // 从微信 CDN 下载
            byte[] bytes = wechatClient.downloadMedia(
                message.getImageEncryptParam(), message.getImageAesKey());
            imageDataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
        } else {
            return "抱歉，没有找到要编辑的图片。";
        }

        // ── ② AI 编辑 ──
        String resultUrl = imageService.edit(message.getText(), imageDataUrl);
        if (resultUrl == null) return "抱歉，图片编辑失败，请稍后再试。";

        // ── ③ 下载结果 → 上传 CDN → 发图（复用 ImageGenerationHandler 的管线） ──
        byte[] resultBytes = downloadImage(resultUrl);
        ILinkClient.MediaInfo mediaInfo = wechatClient.uploadMedia(1, message.getUserId(), resultBytes);
        wechatClient.sendImageMessage(message.getUserId(), message.getContextToken(), mediaInfo);

        return null;  // 不发文字，图已发
    }
}
```

管线跟 `ImageGenerationHandler` 几乎一样：AI 生成 → 下载 → 上传 CDN → 发图。唯一差异是 AI 调用时多传一张原图。

---

## 五、MessageRouter — 加路由

```java
// selectHandler 加一个 case
case IMAGE_EDIT -> imageEditHandler;

// route() 的 IMAGE 分支需要改：如果图片附带文字，不走 VisionHandler，
// 而是走意图分类
if (message.getType() == MessageType.IMAGE) {
    if (message.isHasAttachedImage()) {
        // 图文合并消息，走意图分类（可能是编辑指令）
        Intent intent = intentClassifier.classify(message.getText());
        MessageHandler handler = selectHandler(intent);
        return fallbackIfEmpty(handler.handle(message), message);
    }
    // 纯图片，走 VisionHandler（现有逻辑不变）
    return fallbackIfEmpty(visionHandler.handle(message), message);
}
```

---

## 六、完整数据流

```
用户发：一张猫的照片 + 文字"把背景改成蓝色"
══════════════════════════════════════════════════

1. 微信 SDK: itemList = [IMAGE, TEXT "把背景改成蓝色"]
     ↓
2. pollLoop:
   mergeImageAndText() → 合并为一个 item
   → wechatMsg.type = TEXT
   → wechatMsg.text = "把背景改成蓝色"
   → wechatMsg.imageEncryptParam = "xxx"
   → wechatMsg.imageAesKey = "yyy"
   → wechatMsg.hasAttachedImage = true
     ↓
3. MessageRouter.route(wechatMsg):
   → type == TEXT → intentClassifier.classify("把背景改成蓝色")
   → IMAGE_EDIT
   → imageEditHandler.handle(message)
     ↓
4. ImageEditHandler:
   ① downloadMedia("xxx", "yyy") → byte[] (原图)
   ② Base64 编码 → "data:image/jpeg;base64,..."
   ③ imageService.edit("把背景改成蓝色", dataUrl)
      → ImageClient.editImage("把背景改成蓝色", dataUrl)
        POST https://dashscope.aliyuncs.com/...
        Body: {
          "model": "qwen-image-2.0",
          "input": {"messages": [{
            "role": "user",
            "content": [
              {"image": "data:image/jpeg;base64,..."},  ← 原图
              {"text": "把背景改成蓝色"}                  ← 指令
            ]
          }]},
          "parameters": {"size": "1024*1024", "n": 1}
        }
        Response: {"output": {"choices": [{"message": {"content": [{"image": "https://..."}]}}]}}
      → "https://result-image-url"
   ④ downloadImage(resultUrl) → byte[]
   ⑤ uploadMedia(1, userId, bytes) → MediaInfo
   ⑥ sendImageMessage(userId, contextToken, mediaInfo)
   ⑦ return null
     ↓
5. 用户看到编辑后的图片 ✓
```

---

## 七、和上下文功能的联动

```
第一轮：发图 → VisionHandler 分析 → contextStore 记下 "[用户发了一张图片]" + 分析结果
第二轮：打字"把背景改成蓝色" → LLM 知道"背景"指的是上一张图的背景
        → 但问题是：IMAGE_EDIT 需要图片数据，仅靠文字描述不够

所以编辑场景的最佳实践是：
  - 图文合并（方案主流程）→ 图在手边，直接编辑
  - 引用+文字 → 引用之前的分析结果，LLM 知道在说哪张图，但没有原始图片
```

对于引用+文字的场景（"把刚才那张图的背景改了"），有两种处理：
- **降级方案**：提示用户"请直接发送图片 + 编辑指令"
- **高级方案**：从上下文记住的历史图片 URL 重新下载编辑。这个要额外存储图片 URL/数据，复杂度高，第一期不建议。

---

## 八、和现有功能的对比

| | 文生图 | 图片分析 | 图片编辑 |
|------|--------|---------|---------|
| 输入 | 文字 | 图片 | 图片+文字 |
| 模型 | qwen-image-2.0 | qwen-vl-max | qwen-image-2.0（同一个） |
| 输出 | 图片 | 文字 | 图片 |
| Handler | ImageGenerationHandler | VisionHandler | ImageEditHandler |
| 管线 | 生成→下载→CDN→发图 | 下载→base64→Vision→文字 | 下载→base64→图生图→下载→CDN→发图 |

---

## 九、为什么不直接用引用消息实现

引用消息（refMessage）只传递**文字**，不传图片数据。用户引用一张图片打字"把背景改了"，Handler 拿不到图片的 `encryptQueryParam`——图片是历史数据，CDN 下载链接可能已过期。

图文合并消息的优势：图片是**当前消息的一部分**，CDN 参数新鲜有效，直接下载编辑。
