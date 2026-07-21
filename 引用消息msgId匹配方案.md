# 引用消息 msgId 匹配方案

## 问题

SDK 1.0.1 的 bug：引用消息的 `MessageItemDto` 类型正确（TEXT）但 `getText()` 返回 null，标题也为 null。无法直接获取被引用消息的文字内容。

## 方案

建立 msgId → text 索引，收到每条消息时注册，引用时用 ref 里的 msgId 反查。

## 实现

### 1. 数据模型 — WechatMessage 加 msgId

```java
private String msgId;  // 微信消息 ID，来源 item.getMsgId()
```

### 2. ContextStore 加索引接口

```java
void registerMsgId(String userId, String msgId, String text);  // 存
String lookupMsgId(String userId, String msgId);                 // 查
```

### 3. InMemoryContextStore 实现

```java
// userId → (msgId → text)
ConcurrentHashMap<String, ConcurrentHashMap<String, String>> msgIdIndex;
```

### 4. 收消息时注册

`WechatMessageService.routeAndReply()` 中，每条消息路由前注册：

```java
if (wechatMsg.getMsgId() != null && wechatMsg.getText() != null) {
    contextStore.registerMsgId(fromUserId, wechatMsg.getMsgId(), wechatMsg.getText());
}
```

覆盖场景：用户发送的 TEXT 消息、图文合并消息。

### 5. 引用时按优先级提取

`extractRefMessage()` 改为五级降级：

```
① refItem.getText()           — SDK 正常提供文字时
  → ② lookupMsgId(msgId)      — SDK bug text=null 时用 msgId 查 ContextStore
    → ③ 历史最后 assistant      — bot 消息兜底（bot 发的没注册 msgId）
      → ④ refItem.getVoice()   — 引用语音消息
        → ⑤ ref.getTitle()     — 最终降级
```

## 效果

| 场景 | 提取方式 | 结果 |
|---|---|---|
| 用户引用用户消息 | ② lookupMsgId | ✅ 从索引查到 |
| 用户引用 bot 消息 | ③ history fallback | ✅ 从历史取最后 assistant |
| 用户引用语音消息 | ④ voice.getText() | ✅ 微信转写 |
| 用户引用文件/title | ⑤ getTitle() | ✅ title 降级 |

## 改动文件

| 文件 | 改动 |
|---|---|
| `WechatMessage.java` | 新增 msgId 字段 |
| `ContextStore.java` | 新增 registerMsgId/lookupMsgId 接口 |
| `InMemoryContextStore.java` | 实现 msgId → text 索引 |
| `WechatMessageService.java` | 收消息时注册 msgId，extractRefMessage 五级降级 |
