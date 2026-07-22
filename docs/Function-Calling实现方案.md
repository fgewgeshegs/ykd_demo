# Function Calling 实现方案

> 本方案可直接交给 AI 编码助手执行，请严格按照以下要求实施。

---

## 一、目标

在现有 CHAT 意图处理链路中加入 Function Calling 能力，让大模型能够自主决定是否调用外部工具（如天气查询），并基于工具返回的真实数据生成回复。

**改造范围**：仅改 CHAT 意图内部的 ChatService 调用方式。意图分类器、VoiceTool、MessageRouter、图片/文件/语音处理链路均不改动。

**验证目标**：用户在微信中发"上海天气怎么样"，系统调用真实天气 API，返回真实天气数据。

---

## 二、需要新建的文件

### 文件 1：`src/main/java/com/youkeda/exercise/claw/agent/tool/FunctionTool.java`

这是工具的 Function Calling 扩展接口，所有想被大模型调用的工具都要实现这个接口。

```java
package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Function Calling 工具扩展接口
 *
 * 继承 Tool 接口，新增三个方法：
 * 1. 是否支持 Function Calling
 * 2. 返回 OpenAI tools 格式的工具定义（JSON）
 * 3. 接收 JSON 参数执行工具，返回 JSON 结果
 *
 * 不支持 Function Calling 的工具无需实现此接口，原有 Tool 接口不变。
 */
public interface FunctionTool extends Tool {

    /**
     * 返回 OpenAI tools 格式的工具定义 JSON 字符串。
     *
     * 示例格式：
     * {
     *   "type": "function",
     *   "function": {
     *     "name": "get_weather",
     *     "description": "查询指定城市的当前天气信息",
     *     "parameters": {
     *       "type": "object",
     *       "properties": {
     *         "city": { "type": "string", "description": "城市名称" }
     *       },
     *       "required": ["city"]
     *     }
     *   }
     * }
     */
    String getToolDefinition();

    /**
     * 执行 Function Calling 调用
     *
     * @param arguments 大模型返回的 JSON 参数，如 {"city": "上海"}
     * @return JSON 格式的执行结果，如 {"city":"上海","weather":"晴","temperature":28}
     */
    String executeFunction(JsonNode arguments);
}
```

### 文件 2：`src/main/java/com/youkeda/exercise/claw/agent/tool/WeatherFunctionTool.java`

天气工具的 Function Calling 实现，复用现有 `WeatherTool` 的查询逻辑。

```java
package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.classify.Intent;
import com.youkeda.exercise.claw.weather.WeatherResponse;
import com.youkeda.exercise.claw.weather.WeatherTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 天气 Function Calling 工具
 *
 * 复用 WeatherTool 的查询逻辑，提供 OpenAI Function Calling 接口。
 * 系统启动时自动注册到 ToolRegistry。
 */
@Slf4j
@Component
public class WeatherFunctionTool implements FunctionTool {

    private static final String TOOL_NAME = "get_weather";

    private static final String TOOL_DEFINITION = """
            {
                "type": "function",
                "function": {
                    "name": "get_weather",
                    "description": "查询指定城市的当前天气信息，包括天气状况、温度、湿度。当用户询问天气相关问题时调用此工具。",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "city": {
                                "type": "string",
                                "description": "城市名称，如：上海、北京、Tokyo、New York"
                            }
                        },
                        "required": ["city"]
                    }
                }
            }
            """;

    private final WeatherTool weatherTool;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public WeatherFunctionTool(WeatherTool weatherTool, ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.weatherTool = weatherTool;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        toolRegistry.register(this);
        log.info("WeatherFunctionTool 已注册（Function Calling）");
    }

    @Override
    public String getToolDefinition() {
        return TOOL_DEFINITION;
    }

    @Override
    public String executeFunction(JsonNode arguments) {
        try {
            JsonNode cityNode = arguments.get("city");
            if (cityNode == null || cityNode.asText().isEmpty()) {
                return "{\"error\": \"缺少 city 参数\"}";
            }
            String city = cityNode.asText();
            log.info("Function Calling: get_weather | city={}", city);

            WeatherResponse response = weatherTool.queryWeather(city);

            return objectMapper.writeValueAsString(Map.of(
                    "city", response.getCity(),
                    "weather", response.getWeather(),
                    "temperature", response.getTemperature(),
                    "humidity", response.getHumidity()
            ));
        } catch (Exception e) {
            log.error("WeatherFunctionTool 执行失败 | error={}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // ===== Tool 接口实现（保留原有兼容） =====

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "查询城市天气信息（Function Calling）";
    }

    @Override
    public Intent[] supportedIntents() {
        return new Intent[]{Intent.CHAT};
    }

    @Override
    public String execute(com.youkeda.exercise.claw.agent.AgentContext context) {
        // Agent 体系的 execute 不走这里，走 executeFunction
        return "请通过 Function Calling 调用此工具";
    }
}
```

### 文件 3：`src/main/java/com/youkeda/exercise/claw/ai/llm/ToolCall.java`

大模型返回的工具调用请求的数据模型。

```java
package com.youkeda.exercise.claw.ai.llm;

/**
 * 大模型返回的工具调用请求
 *
 * @param id           调用 ID（用于第二轮请求中关联 tool 结果）
 * @param functionName 工具名称，如 "get_weather"
 * @param arguments    参数 JSON 字符串，如 "{\"city\":\"上海\"}"
 */
public record ToolCall(String id, String functionName, String arguments) {
}
```

### 文件 4：`src/main/java/com/youkeda/exercise/claw/ai/llm/ToolResult.java`

工具执行结果的数据模型，用于第二轮发给大模型。

```java
package com.youkeda.exercise.claw.ai.llm;

/**
 * 工具执行结果
 *
 * @param toolCallId 对应的 ToolCall.id
 * @param content    工具返回的 JSON 结果字符串
 */
public record ToolResult(String toolCallId, String content) {
}
```

### 文件 5：`src/main/java/com/youkeda/exercise/claw/ai/llm/LLMResponse.java`

LLM 响应的统一模型，区分是文本回复还是工具调用请求。

```java
package com.youkeda.exercise.claw.ai.llm;

import java.util.List;

/**
 * LLM 响应统一模型
 *
 * 大模型返回两种可能：
 * 1. 直接文本回复（不需要调工具）
 * 2. 工具调用请求（需要先调工具再发第二轮）
 */
public class LLMResponse {

    private final Type type;
    private final String text;
    private final List<ToolCall> toolCalls;

    private LLMResponse(Type type, String text, List<ToolCall> toolCalls) {
        this.type = type;
        this.text = text;
        this.toolCalls = toolCalls;
    }

    public static LLMResponse text(String text) {
        return new LLMResponse(Type.TEXT, text, null);
    }

    public static LLMResponse toolCalls(List<ToolCall> toolCalls) {
        return new LLMResponse(Type.TOOL_CALLS, null, toolCalls);
    }

    public boolean isText() { return type == Type.TEXT; }
    public boolean isToolCalls() { return type == Type.TOOL_CALLS; }

    public String getText() { return text; }
    public List<ToolCall> getToolCalls() { return toolCalls; }

    public enum Type { TEXT, TOOL_CALLS }
}
```

---

## 三、需要修改的文件

### 修改 1：`src/main/java/com/youkeda/exercise/claw/agent/tool/ToolRegistry.java`

新增方法：获取所有 FunctionTool 的工具定义、按名称查找工具。

**在现有代码基础上新增以下内容**：

```java
// 新增 import
import com.fasterxml.jackson.databind.JsonNode;
import com.youkeda.exercise.claw.agent.tool.FunctionTool;
import java.util.ArrayList;
import java.util.List;

// 在 ToolRegistry 类中新增以下字段和方法

/**
 * 所有已注册的 FunctionTool 列表
 */
private final List<FunctionTool> functionTools = new ArrayList<>();

/**
 * 注册工具（修改原有 register 方法，在末尾增加 FunctionTool 检测）
 */
public synchronized void register(Tool tool) {
    tools.add(tool);
    for (Intent intent : tool.supportedIntents()) {
        Tool existing = toolMap.put(intent, tool);
        if (existing != null) {
            log.warn("意图 {} 的原工具 {} 被新工具 {} 覆盖", intent, existing.name(), tool.name());
        }
    }
    // 新增：自动收集 FunctionTool
    if (tool instanceof FunctionTool ft) {
        functionTools.add(ft);
        log.info("FunctionTool 已注册: name={}", ft.name());
    }
    log.info("工具已注册: name={}, intents={}", tool.name(), tool.supportedIntents());
}

/**
 * 获取所有 FunctionTool 的工具定义列表（OpenAI tools 格式）
 *
 * @return 工具定义 JSON 字符串列表，供 LLMClient 构建请求体时使用
 */
public synchronized List<String> getFunctionToolDefinitions() {
    List<String> defs = new ArrayList<>();
    for (FunctionTool ft : functionTools) {
        defs.add(ft.getToolDefinition());
    }
    return defs;
}

/**
 * 根据工具名查找 FunctionTool
 *
 * @param functionName 工具名称，如 "get_weather"
 * @return 匹配的 FunctionTool，未找到返回 null
 */
public synchronized FunctionTool findFunctionTool(String functionName) {
    for (FunctionTool ft : functionTools) {
        if (ft.name().equals(functionName)) {
            return ft;
        }
    }
    return null;
}
```

### 修改 2：`src/main/java/com/youkeda/exercise/claw/ai/llm/LLMClient.java`

**改动 A**：新增一个方法 `chatWithTools`，支持传入 tools 定义，返回 `LLMResponse`（区分文本和工具调用）。

```java
/**
 * 带 Function Calling 的 LLM 调用
 *
 * @param userId   用户标识
 * @param text     用户消息
 * @param history  历史消息
 * @param toolDefs 工具定义 JSON 列表（OpenAI tools 格式）
 * @return LLMResponse，可能是文本回复或工具调用请求
 */
public LLMResponse chatWithTools(String userId, String text, List<Message> history, List<String> toolDefs) {
    try {
        String requestBody = buildRequestBodyWithTools(systemPrompt, text, history, toolDefs);
        log.info("调用LLM（带 tools）| message={} | historySize={} | toolsCount={}",
                text, history.size(), toolDefs.size());

        String url = properties.getBaseUrl() + "/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseLLMResponse(response.body());

    } catch (Exception e) {
        log.error("LLM调用失败（带 tools）: {}", e.getMessage());
        return null;
    }
}

/**
 * 第二轮调用：携带工具执行结果，让 LLM 生成最终回复
 *
 * @param userId       用户标识
 * @param text         原始用户消息
 * @param history      历史消息
 * @param toolDefs     工具定义
 * @param toolCalls    第一轮 LLM 返回的工具调用请求
 * @param toolResults  工具执行结果
 * @return LLM 最终文本回复
 */
public String chatWithToolResults(String userId, String text, List<Message> history,
                                   List<String> toolDefs, List<ToolCall> toolCalls,
                                   List<ToolResult> toolResults) {
    try {
        String requestBody = buildRequestBodyWithToolResults(systemPrompt, text, history, toolDefs, toolCalls, toolResults);
        log.info("调用LLM（第二轮，带工具结果）| message={}", text);

        String url = properties.getBaseUrl() + "/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response.body());

    } catch (Exception e) {
        log.error("LLM第二轮调用失败: {}", e.getMessage());
        return null;
    }
}
```

**改动 B**：新增两个构建请求体的私有方法。

```java
/**
 * 构建带 tools 定义的请求体
 */
private String buildRequestBodyWithTools(String systemPrompt, String text,
                                          List<Message> history, List<String> toolDefs) throws Exception {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("model", properties.getModel());

    ArrayNode messages = root.putArray("messages");

    // system prompt
    ObjectNode systemMsg = messages.addObject();
    systemMsg.put("role", "system");
    systemMsg.put("content", systemPrompt);

    // history
    for (Message msg : history) {
        ObjectNode historyMsg = messages.addObject();
        historyMsg.put("role", msg.role());
        historyMsg.put("content", msg.content());
    }

    // user message
    ObjectNode userMsg = messages.addObject();
    userMsg.put("role", "user");
    userMsg.put("content", text);

    // tools
    if (toolDefs != null && !toolDefs.isEmpty()) {
        ArrayNode tools = root.putArray("tools");
        for (String def : toolDefs) {
            tools.add(objectMapper.readTree(def));
        }
    }

    return objectMapper.writeValueAsString(root);
}

/**
 * 构建带工具执行结果的请求体（第二轮）
 *
 * 消息结构：
 *   system → history → user → assistant(tool_calls) → tool(结果) × N
 */
private String buildRequestBodyWithToolResults(String systemPrompt, String text,
                                                List<Message> history, List<String> toolDefs,
                                                List<ToolCall> toolCalls,
                                                List<ToolResult> toolResults) throws Exception {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("model", properties.getModel());

    ArrayNode messages = root.putArray("messages");

    // system
    ObjectNode systemMsg = messages.addObject();
    systemMsg.put("role", "system");
    systemMsg.put("content", systemPrompt);

    // history
    for (Message msg : history) {
        ObjectNode historyMsg = messages.addObject();
        historyMsg.put("role", msg.role());
        historyMsg.put("content", msg.content());
    }

    // user
    ObjectNode userMsg = messages.addObject();
    userMsg.put("role", "user");
    userMsg.put("content", text);

    // assistant with tool_calls
    ObjectNode assistantMsg = messages.addObject();
    assistantMsg.put("role", "assistant");
    assistantMsg.putNull("content");
    ArrayNode toolCallsArray = assistantMsg.putArray("tool_calls");
    for (ToolCall tc : toolCalls) {
        ObjectNode tcNode = toolCallsArray.addObject();
        tcNode.put("id", tc.id());
        tcNode.put("type", "function");
        ObjectNode func = tcNode.putObject("function");
        func.put("name", tc.functionName());
        func.put("arguments", tc.arguments());
    }

    // tool results
    for (ToolResult tr : toolResults) {
        ObjectNode toolMsg = messages.addObject();
        toolMsg.put("role", "tool");
        toolMsg.put("tool_call_id", tr.toolCallId());
        toolMsg.put("content", tr.content());
    }

    // tools（第二轮也需要带）
    if (toolDefs != null && !toolDefs.isEmpty()) {
        ArrayNode tools = root.putArray("tools");
        for (String def : toolDefs) {
            tools.add(objectMapper.readTree(def));
        }
    }

    return objectMapper.writeValueAsString(root);
}
```

**改动 C**：新增解析 LLM 响应的私有方法，区分文本和工具调用。

```java
/**
 * 解析 LLM 响应，返回 LLMResponse（区分 text 和 tool_calls）
 */
private LLMResponse parseLLMResponse(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);

    JsonNode choices = root.get("choices");
    if (choices == null || !choices.isArray() || choices.isEmpty()) {
        log.warn("LLM 响应格式异常: {}", responseBody);
        return null;
    }

    JsonNode message = choices.get(0).get("message");

    // 检查是否有 tool_calls
    JsonNode toolCallsNode = message.get("tool_calls");
    if (toolCallsNode != null && toolCallsNode.isArray() && toolCallsNode.size() > 0) {
        List<ToolCall> calls = new ArrayList<>();
        for (JsonNode tc : toolCallsNode) {
            String id = tc.get("id").asText();
            String funcName = tc.get("function").get("name").asText();
            String args = tc.get("function").get("arguments").asText();
            calls.add(new ToolCall(id, funcName, args));
        }
        log.info("LLM 返回工具调用请求 | count={}", calls.size());
        return LLMResponse.toolCalls(calls);
    }

    // 普通文本回复
    JsonNode content = message.get("content");
    String text = (content != null && !content.isNull()) ? content.asText() : null;
    return LLMResponse.text(text);
}
```

### 修改 3：`src/main/java/com/youkeda/exercise/claw/ai/chat/ChatService.java`

**改动**：新增 `chatWithTools` 方法，实现两轮调用逻辑。原有 `chat` 方法保持不变（兼容不需要 Function Calling 的场景）。

```java
// 新增 import
import com.youkeda.exercise.claw.ai.llm.*;
import com.youkeda.exercise.claw.agent.tool.FunctionTool;
import com.youkeda.exercise.claw.agent.tool.ToolRegistry;
import java.util.ArrayList;
import java.util.List;

// ChatService 类中新增以下字段和方法

private final ToolRegistry toolRegistry;

// 修改构造函数，注入 ToolRegistry
public ChatService(LLMClient llmClient, ContextStore contextStore, ToolRegistry toolRegistry) {
    this.llmClient = llmClient;
    this.contextStore = contextStore;
    this.toolRegistry = toolRegistry;
}

/**
 * 带 Function Calling 的聊天（主入口）
 *
 * 流程：
 * 1. 收集所有 FunctionTool 的工具定义
 * 2. 如果没有注册任何 FunctionTool，退化为普通 chat
 * 3. 第一轮 LLM 调用（带 tools）
 * 4. 如果 LLM 返回文本 → 直接回复
 * 5. 如果 LLM 返回 tool_calls → 执行工具 → 第二轮 LLM 调用 → 生成最终回复
 */
public String chatWithTools(String userId, String message) {
    log.info("ChatService.chatWithTools 开始处理 | user={} | text={}", userId, message);

    try {
        // 1. 收集工具定义
        List<String> toolDefs = toolRegistry.getFunctionToolDefinitions();

        // 2. 没有 FunctionTool 则退化为普通 chat
        if (toolDefs.isEmpty()) {
            log.debug("无 FunctionTool 注册，退化为普通 chat");
            return chat(userId, message);
        }

        // 3. 获取历史
        List<Message> history = contextStore.getHistory(userId, MAX_HISTORY);

        // 4. 第一轮 LLM 调用
        LLMResponse llmResponse = llmClient.chatWithTools(userId, message, history, toolDefs);
        if (llmResponse == null) {
            log.warn("LLM 第一轮调用返回 null");
            return null;
        }

        // 5. 直接文本回复
        if (llmResponse.isText()) {
            String reply = llmResponse.getText();
            if (reply != null && !reply.isEmpty()) {
                contextStore.append(userId, "assistant", reply);
                log.info("ChatService.chatWithTools 直接文本回复 | user={}", userId);
            }
            return reply;
        }

        // 6. 工具调用
        if (llmResponse.isToolCalls()) {
            List<ToolCall> toolCalls = llmResponse.getToolCalls();
            List<ToolResult> toolResults = new ArrayList<>();

            for (ToolCall call : toolCalls) {
                log.info("执行工具调用 | tool={} | args={}", call.functionName(), call.arguments());
                FunctionTool tool = toolRegistry.findFunctionTool(call.functionName());
                if (tool != null) {
                    try {
                        com.fasterxml.jackson.databind.JsonNode args =
                                objectMapper.readTree(call.arguments());
                        String result = tool.executeFunction(args);
                        toolResults.add(new ToolResult(call.id(), result));
                        log.info("工具执行成功 | tool={} | result={}", call.functionName(), result);
                    } catch (Exception e) {
                        log.error("工具执行异常 | tool={} | error={}", call.functionName(), e.getMessage());
                        toolResults.add(new ToolResult(call.id(), "{\"error\": \"" + e.getMessage() + "\"}"));
                    }
                } else {
                    log.warn("未找到工具: {}", call.functionName());
                    toolResults.add(new ToolResult(call.id(), "{\"error\": \"工具不存在: " + call.functionName() + "\"}"));
                }
            }

            // 7. 第二轮 LLM 调用
            String finalReply = llmClient.chatWithToolResults(
                    userId, message, history, toolDefs, toolCalls, toolResults);

            if (finalReply != null && !finalReply.isEmpty()) {
                contextStore.append(userId, "assistant", finalReply);
                log.info("ChatService.chatWithTools 工具调用后回复 | user={}", userId);
            }
            return finalReply;
        }

        return null;

    } catch (Exception e) {
        log.error("ChatService.chatWithTools 异常 | user={} | error={}", userId, e.getMessage());
        // 降级为普通 chat
        return chat(userId, message);
    }
}
```

注意：`ChatService` 需要注入 `ObjectMapper`（用于解析工具参数 JSON）。检查现有构造函数是否已有，如果没有则补充注入。

### 修改 4：`src/main/java/com/youkeda/exercise/claw/agent/tool/ChatTool.java`

**改动**：`handle` 方法中调用 `chatWithTools` 替代 `chat`。

将：
```java
String reply = chatService.chat(message.getUserId(), message.getText());
```

改为：
```java
String reply = chatService.chatWithTools(message.getUserId(), message.getText());
```

`execute` 方法同理：
```java
// 原来
return chatService.chat(context.getUserId(), context.getMessage());
// 改为
return chatService.chatWithTools(context.getUserId(), context.getMessage());
```

---

## 四、不需要修改的文件

以下文件**零改动**：

- `MessageRouter.java` — 路由逻辑不变
- `LLMIntentClassifier.java` — 意图分类不变，天气问题仍分类为 CHAT
- `VoiceTool.java` — 语音链路不变，ChatTool 内部改了就自动生效
- `WechatMessageService.java` — 消息轮询不变
- `WechatILinkClient.java` — 微信客户端不变
- `Intent.java` — 不需要新增意图
- `application.properties` — 不需要新增配置项（复用现有 llm.* 配置）
- `WeatherTool.java` / `WeatherCommand.java` — 原有 CLI 天气功能保持不变

---

## 五、执行顺序

请按以下顺序实施，每完成一步验证一次：

### 第 1 步：新建数据模型

创建以下 3 个文件：
- `FunctionTool.java`（接口）
- `ToolCall.java`（record）
- `ToolResult.java`（record）
- `LLMResponse.java`（类）

编译验证：`mvn compile` 无报错。

### 第 2 步：修改 ToolRegistry

在 `ToolRegistry.java` 中：
- `register` 方法末尾增加 `instanceof FunctionTool` 检测
- 新增 `functionTools` 字段
- 新增 `getFunctionToolDefinitions()` 方法
- 新增 `findFunctionTool(String)` 方法

编译验证：`mvn compile` 无报错。

### 第 3 步：新建 WeatherFunctionTool

创建 `WeatherFunctionTool.java`，实现 `FunctionTool` 接口，`@PostConstruct` 中自动注册。

编译验证：`mvn compile` 无报错。

### 第 4 步：修改 LLMClient

在 `LLMClient.java` 中新增：
- `chatWithTools()` 方法
- `chatWithToolResults()` 方法
- `buildRequestBodyWithTools()` 私有方法
- `buildRequestBodyWithToolResults()` 私有方法
- `parseLLMResponse()` 私有方法

原有 `chat()`、`callLLM()`、`buildRequestBody()`、`parseResponse()` 方法保持不变。

编译验证：`mvn compile` 无报错。

### 第 5 步：修改 ChatService

在 `ChatService.java` 中：
- 构造函数注入 `ToolRegistry` 和 `ObjectMapper`
- 新增 `chatWithTools()` 方法
- 原有 `chat()` 方法保持不变

编译验证：`mvn compile` 无报错。

### 第 6 步：修改 ChatTool

在 `ChatTool.java` 中：
- `handle()` 方法：`chatService.chat(...)` → `chatService.chatWithTools(...)`
- `execute()` 方法：同上

编译验证：`mvn compile` 无报错。

### 第 7 步：整体验证

```bash
mvn clean compile
mvn package -DskipTests
mvn spring-boot:run
```

在微信中测试：
1. 发送"上海天气怎么样" → 期望返回真实天气数据
2. 发送"你好" → 期望正常聊天回复（不调工具）
3. 发送"画一只猫" → 期望走 IMAGE_GENERATE 意图（不受影响）
4. 发送语音问天气 → 期望 ASR → CHAT → Function Calling → TTS 回复

---

## 六、时序图

```
用户: "上海天气怎么样"
  │
  ├─ WechatMessageService.saveMessageToContext()     ← 存用户消息
  │
  ├─ MessageRouter.route()
  │   ├─ LLMIntentClassifier.classify()              ← 第1次LLM: 意图分类
  │   │   └─ 返回 Intent.CHAT
  │   │
  │   └─ ChatTool.handle()
  │       └─ ChatService.chatWithTools()
  │           ├─ toolRegistry.getFunctionToolDefinitions()  ← 收集工具定义
  │           ├─ contextStore.getHistory(20)
  │           │
  │           ├─ LLMClient.chatWithTools()            ← 第2次LLM: 带tools
  │           │   └─ 返回 tool_calls: [get_weather(city="上海")]
  │           │
  │           ├─ WeatherFunctionTool.executeFunction() ← 执行天气API
  │           │   └─ GET openweathermap → {"city":"上海","weather":"晴",...}
  │           │
  │           ├─ LLMClient.chatWithToolResults()      ← 第3次LLM: 带工具结果
  │           │   └─ 返回 "上海今天天气晴朗，28℃..."
  │           │
  │           └─ contextStore.append(assistant回复)
  │
  └─ sendReply() → 发送文字回复

总计：3次 LLM 调用（分类1次 + Function Calling 2次）
```

---

## 七、降级策略

`ChatService.chatWithTools()` 内部有完整的降级保护：

1. **无 FunctionTool 注册** → 退化为普通 `chat()`，零影响
2. **第一轮 LLM 调用失败**（返回 null）→ 退化为普通 `chat()`
3. **工具执行异常** → 返回 error JSON 给 LLM，LLM 基于错误信息回复用户
4. **第二轮 LLM 调用失败** → 退化为普通 `chat()`
5. **整个 chatWithTools 异常** → catch 块中退化为 `chat()`

任何一步失败都不会导致用户收不到回复。
