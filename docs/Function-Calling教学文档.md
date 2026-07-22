# Function Calling 教学文档

## 目录

1. [你现在有的是什么 vs Function Calling 是什么](#1-你现在有的是什么-vs-function-calling-是什么)
2. [核心概念：3 个角色 + 1 个循环](#2-核心概念3-个角色--1-个循环)
3. [OpenAI 协议的 Function Calling 报文格式](#3-openai-协议的-function-calling-报文格式)
4. [动手实现：从现有代码长出 Function Calling](#4-动手实现从现有代码长出-function-calling)
5. [进阶：多轮 Tool Call 循环（ReAct 模式）](# Why)
6. [进阶：并行 Tool Call](#6-进阶并行-tool-call)
7. [进阶：流式 Function Calling](#7-进阶流式-function-calling)
8. [给现有项目的最小侵入改造方案](#8-给现有项目的最小侵入改造方案)

---

## 1. 你现在有的是什么 vs Function Calling 是什么

### 当前架构：**意图分类 + 手动路由**

```
用户输入 "帮我画一只猫"
  │
  ▼
IntentClassifier.classify()  ←── 一次 LLM 调用，只返回 CHAT/IMAGE_GENERATE/...
  │
  ▼
MessageRouter.route(intent)  ←── Java switch/case，硬编码路由
  │
  ▼
Tool.execute()               ←── 执行具体工具
```

**问题**：
- 路由逻辑是**硬编码**的（switch/case），加工具要改代码
- 分类只返回**枚举值**，不包含参数。比如 "3 分钟后提醒我开会"→ `SET_REMINDER`，但 **"3 分钟后"** 和 **"开会"** 这两个参数丢失了
- LLM 只被用来做**分类**，没有发挥推理能力

### Function Calling 架构：**LLM 自己决定调什么、传什么参**

```
用户输入 "帮我画一只猫"
  │
  ▼
发送给 LLM：
  - 用户消息："帮我画一只猫"
  - 可用工具列表：[
      {name:"generate_image", params:{prompt:"图片描述"}},
      {name:"send_message",   params:{text:"回复内容"}},
      {name:"set_reminder",   params:{time:"时间", content:"内容"}}
    ]
  │
  ▼
LLM 返回：
  {
    "tool_calls": [{
      "function": {
        "name": "generate_image",
        "arguments": "{\"prompt\":\"一只可爱的卡通猫\"}"
      }
    }]
  }
  │
  ▼
你的代码执行 generate_image("一只可爱的卡通猫")
  │
  ▼
把结果发回给 LLM → LLM 生成最终回复："好的，已为你生成了一张猫的图片：[url]"
```

**核心区别**：

| | 意图分类 | Function Calling |
|---|---|---|
| 路由决策 | Java switch/case | LLM 推理决定 |
| 参数提取 | ❌ 不支持 | ✅ LLM 自己填 JSON |
| 加新工具 | 改枚举 + switch + Handler | 只加 Tool Bean |
| 多工具组合 | ❌ | ✅ 一次对话调多个工具 |
| 动态纠错 | ❌ | ✅ 工具失败后 LLM 可换策略 |

---

## 2. 核心概念：3 个角色 + 1 个循环

### 三个角色

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   User       │     │   LLM       │     │   Tool      │
│              │     │             │     │             │
│ 发消息的人    │     │ 决策的大脑   │     │ 干活的手     │
└─────────────┘     └─────────────┘     └─────────────┘
      │                    │                    │
      │  ①"帮我查天气"     │                    │
      │──────────────────▶│                    │
      │                    │                    │
      │                    │  ② call            │
      │                    │  get_weather(      │
      │                    │    city:"杭州")    │
      │                    │──────────────────▶│
      │                    │                    │
      │                    │  ③ "晴, 25°C"     │
      │                    │◀──────────────────│
      │                    │                    │
      │  ④"杭州今天晴,25°C" │                    │
      │◀──────────────────│                    │
```

- **User**：发消息的人
- **LLM**：决策大脑，决定要不要调工具、调哪个、传什么参数
- **Tool**：干活的手，执行具体操作，返回结果

### 一个循环

Function Calling 不是一次性的事。LLM 调完工具后，可能还需要再调下一个工具。所以是一个 **循环**：

```java
while (true) {
    response = llm.chat(messages, tools);  // 发消息 + 可用工具列表
    if (response.isFinished()) break;       // LLM 觉得任务完成了
    if (response.hasToolCalls()) {
        for (ToolCall call : response.getToolCalls()) {
            result = tool.execute(call.name, call.arguments);
            messages.add(toolResult(result));  // 工具结果追加到会话
        }
    }
}
```

这就是 **ReAct（Reasoning + Acting）循环**——也是你项目中 `SimpleAgentExecutor` 注释里提到的演进方向。

---

## 3. OpenAI 协议的 Function Calling 报文格式

你项目用的 `LLMClient` 调用的是 `/chat/completions` 端点，兼容 OpenAI 协议。Function Calling 只是在这个协议上加了 `tools` 字段。

### 3.1 请求体：多了 `tools` 字段

```json
{
  "model": "gpt-4",
  "messages": [
    {"role": "system", "content": "你是一个助手，可以使用工具。"},
    {"role": "user",   "content": "杭州今天天气怎么样？"}
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "查询指定城市的实时天气",
        "parameters": {
          "type": "object",
          "properties": {
            "city": {
              "type": "string",
              "description": "城市名（中文）"
            }
          },
          "required": ["city"]
        }
      }
    }
  ]
}
```

`tools` 数组告诉 LLM："你有这些工具可以用，每个工具的参数 schema 是这样的。"

### 3.2 响应体：LLM 可能返回 tool_calls

```json
// LLM 决定调工具：
{
  "choices": [{
    "message": {
      "role": "assistant",
      "content": null,              // ← 调工具时 content 为 null
      "tool_calls": [{
        "id": "call_abc123",
        "type": "function",
        "function": {
          "name": "get_weather",
          "arguments": "{\"city\":\"杭州\"}"  // ← JSON 字符串
        }
      }]
    }
  }]
}
```

```json
// LLM 决定直接回复文本：
{
  "choices": [{
    "message": {
      "role": "assistant",
      "content": "想了解什么城市的天气呢？"  // ← 直接回复
    }
  }]
}
```

### 3.3 工具执行结果发回给 LLM

当你执行完工具，把结果追加到 messages 数组：

```json
{
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "杭州天气？"},
    {"role": "assistant", "content": null, "tool_calls": [...]},
    {
      "role": "tool",
      "tool_call_id": "call_abc123",
      "content": "杭州今天晴，25°C，湿度 60%"
    }
  ]
}
```

LLM 收到工具结果后，生成最终回复：
```json
{"role": "assistant", "content": "杭州今天天气晴朗，气温25°C，湿度60%。"}
```

---

## 4. 动手实现：从现有代码长出 Function Calling

### Step 1：把 Tool 接口升级，增加 JSON Schema

你现在 Tool 接口是：

```java
public interface Tool {
    String name();
    String description();
    Intent[] supportedIntents();
    String execute(AgentContext context);
}
```

Function Calling 需要的是**参数 Schema**（LLM 用它来判断要传什么参数）。加一个方法：

```java
/**
 * 工具参数 JSON Schema
 *
 * LLM 用这个 Schema 理解应该传什么参数。
 * 返回 null 表示无参数工具。
 *
 * 示例：
 * {
 *   "type": "object",
 *   "properties": {
 *     "city": {"type": "string", "description": "城市名（中文）"}
 *   },
 *   "required": ["city"]
 * }
 */
default String parameterSchema() {
    return null;  // 默认无参数
}
```

### Step 2：修改 LLMClient，支持发送 tools 和解析 tool_calls

你现在 `LLMClient.callLLM()` 只构建 `messages` 数组，需要扩展：

```java
/**
 * 支持 Function Calling 的 LLM 调用
 *
 * @param systemPrompt 系统提示词
 * @param text         用户消息
 * @param history      历史消息（含之前的 tool call 结果）
 * @param tools        可用工具列表（null = 普通对话）
 * @return LLM 响应（可能包含 tool_calls）
 */
public LLMResponse chatWithTools(String systemPrompt, String text,
                                  List<Message> history,
                                  List<Map<String, Object>> tools) {
    try {
        String requestBody = buildRequestBodyWithTools(systemPrompt, text, history, tools);
        // ... HTTP 调用同原来 ...
        return parseResponseWithToolCalls(response.body());
    } catch (Exception e) {
        log.error("LLM 工具调用失败: {}", e.getMessage());
        return null;
    }
}
```

**核心改动在 `buildRequestBodyWithTools`**：

```java
private String buildRequestBodyWithTools(String systemPrompt, String text,
                                          List<Message> history,
                                          List<Map<String, Object>> tools) throws Exception {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("model", properties.getModel());

    ArrayNode messages = root.putArray("messages");

    // system prompt
    ObjectNode sys = messages.addObject();
    sys.put("role", "system");
    sys.put("content", systemPrompt);

    // history（关键：这里可以包含以前的 assistant/tool 消息）
    for (Message msg : history) {
        ObjectNode m = messages.addObject();
        m.put("role", msg.role());
        m.put("content", msg.content());
        // 如果是 tool 消息，要带 tool_call_id
        if ("tool".equals(msg.role()) && msg.toolCallId() != null) {
            m.put("tool_call_id", msg.toolCallId());
        }
    }

    // current user message
    ObjectNode userMsg = messages.addObject();
    userMsg.put("role", "user");
    userMsg.put("content", text);

    // 🔑 关键：把 tools 数组放入请求体
    if (tools != null && !tools.isEmpty()) {
        ArrayNode toolsArray = root.putArray("tools");
        for (Map<String, Object> toolDef : tools) {
            ObjectNode toolNode = toolsArray.addObject();
            toolNode.put("type", "function");
            toolNode.set("function", objectMapper.valueToTree(toolDef));
        }
    }

    return objectMapper.writeValueAsString(root);
}
```

**`parseResponseWithToolCalls` 解析 tool_calls**：

```java
private LLMResponse parseResponseWithToolCalls(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode choices = root.get("choices");
    if (choices == null || choices.size() == 0) return null;

    JsonNode message = choices.get(0).get("message");
    if (message == null) return null;

    LLMResponse response = new LLMResponse();

    // 文本回复
    JsonNode contentNode = message.get("content");
    if (contentNode != null && !contentNode.isNull()) {
        response.setText(contentNode.asText());
    }

    // 🔑 解析 tool_calls
    JsonNode toolCallsNode = message.get("tool_calls");
    if (toolCallsNode != null && toolCallsNode.isArray()) {
        List<LLMResponse.ToolCall> toolCalls = new ArrayList<>();
        for (JsonNode tc : toolCallsNode) {
            String id = tc.get("id").asText();
            String name = tc.get("function").get("name").asText();
            String arguments = tc.get("function").get("arguments").asText();
            toolCalls.add(new LLMResponse.ToolCall(id, name, arguments));
        }
        response.setToolCalls(toolCalls);
    }

    return response;
}
```

### Step 3：定义 LLMResponse 模型

```java
@Data
public class LLMResponse {
    private String text;                 // 文本回复（无 tool_calls 时有效）
    private List<ToolCall> toolCalls;    // 工具调用列表

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
    public boolean isFinished() {
        return text != null && !text.isEmpty();
    }

    @Data
    @AllArgsConstructor
    public static class ToolCall {
        private String id;          // call_abc123，后续发回结果时需要
        private String name;        // 工具名
        private String arguments;   // JSON 参数字符串
    }
}
```

### Step 4：升级 ToolRegistry，生成 OpenAI 格式的 tools 定义

```java
/**
 * 生成 OpenAI 兼容的 tools 数组（供 LLMClient 使用）
 */
public List<Map<String, Object>> buildToolDefinitions() {
    List<Map<String, Object>> definitions = new ArrayList<>();
    for (Tool tool : getTools()) {
        Map<String, Object> func = new LinkedHashMap<>();
        func.put("name", tool.name());
        func.put("description", tool.description());
        if (tool.parameterSchema() != null) {
            func.put("parameters", parseSchema(tool.parameterSchema()));
        } else {
            func.put("parameters", Map.of("type", "object", "properties", Map.of()));
        }
        definitions.add(func);
    }
    return definitions;
}
```

### Step 5：实现 ReActAgentExecutor（核心循环）

这是最关键的部分——把 Step 1-4 组装成循环：

```java
@Slf4j
@Component
public class ReActAgentExecutor implements AgentExecutor {

    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ContextStore contextStore;
    private final PromptLoader promptLoader;

    private static final int MAX_LOOPS = 5;  // 安全阀：最多 5 轮工具调用

    @Override
    public String execute(AgentContext context) {
        String userId = context.getUserId();
        String userMessage = context.getMessage();

        // 1. 构建初始 messages（系统提示词 + 历史 + 当前消息）
        List<Message> messages = buildInitialMessages(userId, userMessage);

        // 2. 获取可用工具定义
        List<Map<String, Object>> tools = toolRegistry.buildToolDefinitions();

        // 3. ReAct 循环
        for (int loop = 0; loop < MAX_LOOPS; loop++) {
            LLMResponse response = llmClient.chatWithTools(
                "你是一个智能助手，可以使用工具完成任务。",  // system prompt
                userMessage,
                messages,
                tools
            );

            // 3a. LLM 直接回复文本 → 结束
            if (response.isFinished()) {
                contextStore.append(userId, "assistant", response.getText());
                return response.getText();
            }

            // 3b. LLM 要求调工具 → 执行工具
            if (response.hasToolCalls()) {
                for (LLMResponse.ToolCall tc : response.getToolCalls()) {
                    log.info("ReAct: 执行工具 {} | args={}", tc.getName(), tc.getArguments());

                    Tool tool = toolRegistry.findByName(tc.getName());
                    String result;
                    if (tool == null) {
                        result = "错误：未找到工具 " + tc.getName();
                    } else {
                        try {
                            result = tool.execute(buildContext(context, tc.getArguments()));
                        } catch (Exception e) {
                            result = "工具执行失败：" + e.getMessage();
                        }
                    }

                    // 🔑 把工具结果追加到 messages
                    messages.add(Message.tool(tc.getId(), result));
                    log.info("ReAct: 工具 {} 完成 | result={}", tc.getName(), result);
                }
                continue;  // ← 继续循环，让 LLM 看结果决定下一步
            }

            // 3c. 异常：既没有文本也没有 tool_calls
            log.warn("ReAct: LLM 返回空");
            return "抱歉，处理出错，请稍后重试。";
        }

        // 超过最大循环次数
        return "抱歉，任务比预期复杂，请换个方式描述。";
    }
}
```

### 你现在要改的文件清单

| 文件 | 改动 |
|------|------|
| `agent/Tool.java` | 加 `parameterSchema()` 默认方法 |
| `agent/ToolRegistry.java` | 加 `findByName()` 和 `buildToolDefinitions()` |
| `ai/llm/LLMClient.java` | 加 `chatWithTools()` 方法，改请求体和响应解析 |
| `context/Message.java` | 加 `toolCallId` 字段和 `Message.tool(...)` 工厂方法 |
| `agent/LLMResponse.java` | **新文件**，封装 LLM 响应（文本 or tool_calls） |
| `agent/ReActAgentExecutor.java` | **新文件**，ReAct 循环执行器 |

---

## 5. 进阶：多轮 Tool Call 循环（ReAct 模式）

ReAct 循环的精髓在于：**工具执行结果会改变 LLM 的下一步决策**。

### 示例：查天气 → 推荐穿衣

```
用户: "杭州今天适合穿什么？"

循环 1:
  LLM 决定: get_weather(city="杭州")
  执行结果: "晴, 25°C, 微风"

循环 2:
  LLM 看到结果 "25°C" → 不再调工具, 直接回复:
  "杭州今天晴, 25°C, 适合穿短袖加薄外套。"

✅ isFinished() → 结束
```

### 示例：工具失败 → LLM 换策略

```
用户: "杭州今天天气？"

循环 1:
  LLM 决定: get_weather(city="杭州")
  执行结果: "错误：天气服务暂时不可用"

循环 2:
  LLM 看到失败 → 换策略:
  "抱歉，天气服务暂时不可用。建议您查看中国气象局官网了解杭州今日天气。
   或者稍后再问我。"

✅ isFinished() → 结束
```

### 安全阀

`MAX_LOOPS = 5` 防止无限循环（两个工具互相调用导致死循环）。

---

## 6. 进阶：并行 Tool Call

LLM 可以一次返回多个 `tool_calls`（互不依赖的独立操作）：

```json
{
  "tool_calls": [
    {"id": "c1", "function": {"name": "get_weather", "arguments": "{\"city\":\"杭州\"}"}},
    {"id": "c2", "function": {"name": "get_weather", "arguments": "{\"city\":\"北京\"}"}}
  ]
}
```

你的执行器应该**并行**执行这些不依赖的工具：

```java
// 在 ReActAgentExecutor 的循环中
if (response.hasToolCalls()) {
    // 🔑 并行执行所有 tool calls
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    List<Message> results = Collections.synchronizedList(new ArrayList<>());

    for (LLMResponse.ToolCall tc : response.getToolCalls()) {
        futures.add(CompletableFuture.runAsync(() -> {
            Tool tool = toolRegistry.findByName(tc.getName());
            String result = tool.execute(buildContext(context, tc.getArguments()));
            results.add(Message.tool(tc.getId(), result));
        }));
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    messages.addAll(results);
    continue;
}
```

---

## 7. 进阶：流式 Function Calling

流式（streaming）Function Calling 更复杂——`tool_calls` 的 JSON 是**增量**到达的，你需要边收边拼：

```
事件流:
  delta: {"tool_calls":[{"index":0,"id":"call_","type":"function"}]}
  delta: {"tool_calls":[{"index":0,"function":{"name":"get"}}]}
  delta: {"tool_calls":[{"index":0,"function":{"arguments":"_we"}}]}
  delta: {"tool_calls":[{"index":0,"function":{"arguments":"ather"}}]}
  delta: {"tool_calls":[{"index":0,"function":{"arguments":"({\"ci"}}]}
  ...
```

这需要状态机来管理。**建议先用非流式做通，流式是锦上添花。**

---

## 8. 给现有项目的最小侵入改造方案

如果你想先小步验证，最小改动方案是：

### 方案 A：最小验证（只改 LLMClient + 加一个测试）

不改现有的 Tool 体系，只在 `LLMClient` 里验证 Function Calling 能不能通：

```java
// 临时测试
public String testFunctionCalling() {
    List<Map<String, Object>> tools = List.of(Map.of(
        "name", "get_weather",
        "description", "查询天气",
        "parameters", Map.of(
            "type", "object",
            "properties", Map.of("city", Map.of("type", "string", "description", "城市名")),
            "required", List.of("city")
        )
    ));
    // 发送 messages + tools → 看 LLM 返回什么
}
```

通了再逐步改造 Tool 接口和执行器。

### 方案 B：两条腿走路（你现有的意图分类 + Function Calling 共存）

```
                     用户消息
                        │
          ┌─────────────┴─────────────┐
          ▼                           ▼
   简单对话（CHAT）             复杂任务（需工具）
          │                           │
   现有 LLMClient.chat()      新 LLMClient.chatWithTools()
          │                           │
          ▼                           ▼
      直接回复                   ReActAgentExecutor
                                   │
                                   ▼
                              LLM 决策 → Tool 执行 → 循环
```

通过在系统提示词或意图分类中判断：是普通聊天还是需要工具的任务。

### 方案 C：一夜切换（推荐最终态）

把意图分类器（`IntentClassifier`）替换为 Function Calling：

- 删除 `Intent` 枚举和 `LLMIntentClassifier`
- 所有 Handler 改为 Tool 实现
- `MessageRouter` 改为 `ReActAgentExecutor`（LLM 决策路由）
- 简单对话也是一个 Tool（`send_message`）

---

## 总结：关键设计决策

1. **工具用 JSON Schema 描述参数**——这是 LLM 理解"该传什么参数"的唯一方式
2. **tool_call_id 是会话的一部分**——tool 消息必须带 `tool_call_id`，否则 LLM 对不上
3. **安全阀 MAX_LOOPS 必须有**——防止死循环
4. **并行 tool calls 时注意线程安全**——用 `CompletableFuture` 或 `ExecutorService`
5. **先非流式后流式**——Function Calling 的流式解析比纯文本复杂得多

---

## 用这一页回顾整个流程

```
┌────────────────────────────────────────────────────────────┐
│                    ReActAgentExecutor                      │
│                                                           │
│  while (未结束 且 loop < 5) {                              │
│                                                           │
│    ┌─────────────────────────────────────┐                 │
│    │ 1. 发请求给 LLM                      │                │
│    │    - messages: [system, ...history,  │                │
│    │                user, assistant, tool]│                │
│    │    - tools: [get_weather, send_msg,  │                │
│    │              generate_image, ...]    │                │
│    └─────────────────────────────────────┘                 │
│                      │                                    │
│                      ▼                                    │
│    ┌─────────────────────────────────────┐                 │
│    │ 2. 解析 LLM 响应                     │                │
│    │    - content = "..." → 最终回复 → 结束│                │
│    │    - tool_calls = [...] → 继续       │                │
│    └─────────────────────────────────────┘                 │
│                      │                                    │
│                      ▼                                    │
│    ┌─────────────────────────────────────┐                 │
│    │ 3. 执行工具（并行）                   │                │
│    │    ToolRegistry.findByName(name)     │                │
│    │        .execute(context)             │                │
│    └─────────────────────────────────────┘                 │
│                      │                                    │
│                      ▼                                    │
│    ┌─────────────────────────────────────┐                 │
│    │ 4. 追加 tool 结果到 messages          │                │
│    │    messages.add({role:"tool",        │                │
│    │      tool_call_id: id,               │                │
│    │      content: result})              │                │
│    └─────────────────────────────────────┘                 │
│                      │                                    │
│                      ▼                                    │
│              continue → 回到第 1 步                       │
│  }                                                       │
└────────────────────────────────────────────────────────────┘
```
