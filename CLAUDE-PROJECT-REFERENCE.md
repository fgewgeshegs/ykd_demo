# Claw Assistant — 项目全景参考

> 生成日期：2026-07-18
> 分支：`feature/image-generate`
> 作用：为 AI 助手（ChatGPT、Claude 等）提供完整项目上下文，使其能精确理解、修改和扩展本项目。

---

## 一、项目概述

**Claw Assistant** 是一个运行在 **Spring Boot** 上的智能助手应用，同时提供：

- 🖥️ **CLI 控制台** — 传统命令行交互
- 📱 **微信 iLink** — 通过微信收发消息（扫码登录，轮询消息）

核心能力由多个 **大语言模型（LLM）** 驱动，支持文本对话、图片理解、图片生成三种 AI 能力。

### 1.1 核心能力矩阵

| 能力 | 触发方式 | 后端服务 | 模型客户端 | 模型提供商 |
|------|----------|----------|-----------|-----------|
| 💬 文本对话 | 微信文本消息 | `ChatService` | `LLMClient` | DeepSeek / 兼容 OpenAI 协议 |
| 🖼️ 图片理解 | 微信图片消息 | `VisionService` | `VisionClient` | Qwen-VL / OpenAI 多模态协议 |
| 🎨 图片生成 | 微信文本消息（含 "生成图片" 意图） | `ImageGenerationService` | `ImageClient` | Qwen-Image (阿里云 DashScope) |
| 🌤️ 天气查询 | CLI `weather <城市>` | `WeatherTool` | 直接 HTTP 调用 | OpenWeatherMap |

---

## 二、技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 21 | 使用虚拟线程、`HttpClient`、`Record` 等新特性 |
| 框架 | Spring Boot | 3.3.2 | Starter 模式（无 Web 服务器） |
| 构建 | Maven | - | `spring-boot-maven-plugin` 打包 fat JAR |
| 工具库 | Lombok | 最新 | `@Slf4j` `@Data` 简化代码 |
| JSON | Jackson | - | `jackson-databind` |
| 微信 SDK | wechat-ilink-sdk | 1.0.1 | `io.github.lith0924` — 独立微信协议库 |
| HTTP | `java.net.http.HttpClient` | 内置 | Java 21 原生，无需第三方 HTTP 库 |

---

## 三、包结构

```
com.youkeda.exercise.claw
│
├── ClawAssistantApplication.java     # Spring Boot 入口
├── Main.java                         # CLI 主循环（@Component, @PostConstruct 启动）
├── ClawException.java                # 统一业务异常
│
├── ai/                               # 所有 AI 能力
│   ├── chat/ChatService.java         # 文本对话业务封装
│   ├── image/
│   │   ├── ImageClient.java          # 图片生成 HTTP 客户端（DashScope 协议）
│   │   ├── ImageGenerationService.java # 图片生成业务封装
│   │   └── ImageProperties.java      # 图片生成配置（@ConfigurationProperties）
│   ├── vision/VisionService.java     # 图片理解业务封装
│   ├── llm/                          # LLM 底层客户端
│   │   ├── LLMClient.java            # 文本 LLM HTTP 客户端
│   │   ├── VisionClient.java         # 多模态视觉 HTTP 客户端（含 @PostConstruct 加载提示词）
│   │   ├── LLMProperties.java        # LLM 配置（api-key, base-url, model）
│   │   ├── VisionProperties.java     # 视觉模型配置
│   │   └── SystemPromptProvider.java # 系统提示词加载（从 classpath:prompts/system-prompt.txt）
│   └── classifier/                   # 意图分类
│       ├── Intent.java               # 意图枚举: CHAT / IMAGE_GENERATE / IMAGE_ANALYZE
│       ├── IntentClassifier.java     # 分类器接口
│       └── LLMIntentClassifier.java  # 基于 LLM 的分类实现（通过分类 Prompt 判断）
│
├── weather/                          # 天气功能（收拢在一起）
│   ├── WeatherCommand.java           # CLI 命令端
│   ├── WeatherConfig.java            # 配置（@Value 注入）
│   ├── WeatherResponse.java          # 数据模型
│   └── WeatherTool.java              # 业务逻辑 + HTTP 调用
│
├── wechat/                           # 微信 iLink 模块（可选，gated 配置）
│   ├── MessageRouter.java            # 消息路由器（从 router/ 移入）
│   ├── client/WechatILinkClient.java # SDK 封装：登录、收发、下载
│   ├── config/WechatProperties.java  # 配置：enabled、轮询间隔
│   ├── handler/
│   │   ├── MessageHandler.java       # 处理器接口
│   │   ├── AIChatHandler.java        # 文本消息 → ChatService
│   │   ├── VisionHandler.java        # 图片消息 → VisionService（含微信 CDN 下载）
│   │   ├── ImageGenerationHandler.java # 图片生成意图 → ImageGenerationService
│   │   └── SimpleReplyHandler.java   # 兜底回复
│   ├── model/
│   │   ├── MessageType.java          # 消息类型枚举
│   │   └── WechatMessage.java        # 统一消息模型
│   └── service/WechatMessageService.java # 轮询监听服务
│
├── command/                          # CLI 命令
│   ├── CommandHandler.java           # 命令接口
│   ├── HelpCommand.java              # help
│   ├── StatusCommand.java            # status
│   └── VersionCommand.java           # version
│
├── common/                           # 通用工具
│   └── HttpClientUtil.java           # HTTP GET 封装（供 WeatherTool 使用）
│
└── agent/                            # Agent 体系（预留/演进中）
    ├── AgentContext.java             # Agent 执行上下文
    ├── AgentExecutor.java            # Agent 执行器接口
    ├── SimpleAgentExecutor.java      # 当前实现：桥接 MessageRouter
    ├── Tool.java                     # 工具接口
    ├── ToolRegistry.java             # 工具注册中心（Intent → Tool 映射）
    ├── ChatTool.java                 # ChatService 的 Tool 包装
    ├── VisionTool.java               # VisionService 的 Tool 包装
    ├── ImageGenerationTool.java      # ImageGenerationService 的 Tool 包装
    └── ... (其他预留)
```

---

## 四、核心架构与数据流

### 4.1 双子系统架构

应用启动后并行运行两个独立子系统：

```
ClawAssistantApplication.main()
    │
    ├── CLI 子系统（Main.java @PostConstruct）
    │   ├── 注册命令（类名去掉 "Command" 后小写）
    │   └── 启动 daemon 线程，stdin while-true 循环
    │
    └── 微信子系统（如果 enabled）
        ├── WechatILinkClient.init() → 异步登录（QR扫码）
        ├── WechatMessageService.start() → 等待登录（最多60s）
        └── 启动轮询线程，poll 微信消息
```

### 4.2 CLI 命令数据流

```
用户输入 "weather 北京"
    → Main.processCommand()
        → 查找 commandHandlers["weather"] → WeatherCommand
            → WeatherCommand.execute()
                → WeatherTool.queryWeather("北京")
                    → HttpClientUtil.doGet(URL)
                        → OpenWeatherMap API
                    ← JSON → WeatherResponse
                ← WeatherResponse.toString()
            ← System.out.println(格式化天气)
```

### 4.3 微信消息数据流

```
微信用户发送文本
    → WechatILinkClient.receiveMessages(cursor)
        → WechatMessageService.pollLoop()
            → 构建 WechatMessage
                → MessageRouter.route(message)
                    │
                    ├─ 图片消息 → 直接 VisionHandler.handle()
                    │               → VisionService.analyze()
                    │                   → VisionClient.analyzeImage()
                    │                       → HTTP POST 多模态模型
                    │                   ← 图片描述
                    │               ← 回复文本
                    │
                    ├─ 文本消息 → LLMIntentClassifier.classify(text)
                    │               → LLMClient.chatWithSystemPrompt(分类Prompt)
                    │               ← Intent枚举
                    │           → selectHandler(intent)
                    │               ├─ CHAT → AIChatHandler
                    │               │           → ChatService.chat()
                    │               │               → LLMClient.chat()
                    │               │                   → HTTP POST DeepSeek
                    │               │               ← 回复
                    │               │
                    │               ├─ IMAGE_GENERATE → ImageGenerationHandler
                    │               │                   → ImageGenerationService.generate()
                    │               │                       → ImageClient.generateImage()
                    │               │                           → HTTP POST DashScope
                    │               │                       ← 图片URL
                    │               │                   ← "已为您生成图片：{url}"
                    │               │
                    │               └─ IMAGE_ANALYZE → VisionHandler (同上)
                    │
                    └─ 兜底 → SimpleReplyHandler.handle()
                            ← "暂时无法理解该消息类型"
                ← 回复文本
            → WechatILinkClient.sendTextMessage(reply)
```

### 4.4 Agent 体系（演进中）

当前 `agent/` 包是预留架构，实际路由仍走 `MessageRouter`：

```
当前: MessageRouter (意图路由) → Handler → Service → Client
未来: Planner → ToolRegistry.findTool(intent) → Tool.execute() → Service → Client
```

Agent 体系的设计目标：
1. **SimpleAgentExecutor**（当前）— 桥接 `MessageRouter`，不改变行为
2. **PlannerAgentExecutor**（计划）— 引入 Planner 替换 `IntentClassifier`
3. **ReActAgentExecutor**（远期）— 思考-行动-观察循环

---

## 五、配置参考

配置项在 `application.properties.example`（需复制为 `application.properties` 并填入 key）：

### 5.1 天气 OpenWeatherMap

```properties
weather.api.url=https://api.openweathermap.org/data/2.5/weather?q={city}&appid={key}&units=metric&lang=zh_cn
weather.api.key=YOUR_API_KEY
```

### 5.2 微信 iLink

```properties
wechat.ilink.enabled=false           # 设为 true 启用
wechat.ilink.poll-interval-ms=3000   # 消息轮询间隔
wechat.ilink.login-poll-interval-ms=3000  # 登录轮询间隔
```

### 5.3 LLM (文本对话)

```properties
llm.api-key=                         # API 密钥
llm.base-url=https://api.deepseek.com # 兼容 OpenAI 协议的端点
llm.model=deepseek-v4-flash          # 模型名称
```

### 5.4 Vision (图片理解)

```properties
vision.api-key=                      # API 密钥
vision.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
vision.model=qwen-vl-max
```

### 5.5 Image (图片生成)

```properties
image.api-key=                       # API 密钥
image.base-url=https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation
image.model=qwen-image-2.0
```

### 5.6 配置类对应关系

| 配置前缀 | 配置类 | 位置 |
|----------|--------|------|
| `weather.api` | `WeatherConfig`（@Value） | `weather/` |
| `wechat.ilink` | `WechatProperties`（@ConfigurationProperties） | `wechat/config/` |
| `llm.*` | `LLMProperties`（@ConfigurationProperties） | `ai/llm/` |
| `vision.*` | `VisionProperties`（@ConfigurationProperties） | `ai/llm/` |
| `image.*` | `ImageProperties`（@ConfigurationProperties） | `ai/image/` |

---

## 六、资源文件

| 路径 | 用途 |
|------|------|
| `src/main/resources/application.properties` | 实际配置（已 gitignore，不提交） |
| `src/main/resources/application.properties.example` | 配置模板 |
| `src/main/resources/prompts/system-prompt.txt` | LLM 系统提示词 |
| `src/main/resources/prompts/vision-system-prompt.txt` | 视觉模型系统提示词 |

---

## 七、关键设计决策

### 7.1 为什么没有 `spring-boot-starter-web`？

因为不需要 Web 服务器。应用通过 CLI stdin 和微信 SDK 轮询与用户交互，不是 HTTP API 服务。使用 `spring-boot-starter` 足够。

### 7.2 命令如何自动注册？

`Main` 构造函数接收 `List<CommandHandler>`（Spring 自动注入所有实现类），通过 `类名.replace("Command", "").toLowerCase()` 建立命令名→Handler 映射。新增命令只需：
1. 实现 `CommandHandler`
2. 加 `@Component`
3. 类名以 `Command` 结尾

### 7.3 意图分类为什么用 LLM 而非规则？

使用 `LLMIntentClassifier` 调用 LLM 判断用户意图（通过分类专用 Prompt），而非简单关键字匹配。未来演进方向是 **Planner**（Agent 体系中更智能的任务规划器）。

### 7.4 为什么 MessageRouter 直接注入具体 Handler 而非遍历？

早期版本用 `List<MessageHandler>` 按 `@Order` 遍历。重构后改为 **IntentClassifier 先行**，再根据 Intent 选择 Handler，路由更加确定。Handler 不再需要 `@Order`。

### 7.5 图片消息在微信中的处理链路

微信 IMAGE 消息 → 从 CDN 下载加密图片（`encryptQueryParam` + `aesKey`）→ 转为 base64 data URL → 传给多模态模型分析。CDN 下载走微信 iLink SDK 的 `downloadMedia()` 方法。

### 7.6 异常处理原则

- `ClawException` 统一包装业务异常
- 所有 SDK/HTTP 调用都有 try-catch，确保不传播未捕获异常
- CLI 中 `Main.processCommand()` 捕获 `ClawException` 并打印友好提示
- 微信模块中所有异常被捕获并 log，不中断轮询循环

### 7.7 LLM 请求格式兼容性

所有 LLM 调用（文本、多模态、图片生成）都构建兼容 **OpenAI 协议**的请求体，允许更换任意兼容 OpenAI 格式的模型提供商。

---

## 八、构建与运行

```bash
# 前置条件：Java 21, Maven
# 复制配置模板并填入 API Key
cp src/main/resources/application.properties.example src/main/resources/application.properties

# 编译
mvn clean compile

# 打包 fat JAR
mvn package -DskipTests

# 运行（Maven）
mvn spring-boot:run

# 运行（JAR）
java -jar target/claw-assistant-1.0-SNAPSHOT.jar
```

> **注意 JVM 编码**：Windows 中文系统默认编码为 GBK，可能导致提示词文件读取乱码。建议添加启动参数 `-Dfile.encoding=UTF-8`。

---

## 九、演进路线

| 阶段 | 目标 | 关键变更 |
|------|------|----------|
| ✅ 当前 | 双子系统 + 三大 AI 能力 | CLI / 微信 / LLM / Vision / Image |
| 🏗️ 进行中 | Agent 体系落地 | `SimpleAgentExecutor` → `PlannerAgentExecutor` |
| 🗺️ 规划 | ReAct Agent | 思考-行动-观察循环 |
| 🗺️ 规划 | 多轮对话上下文 | `AgentContext.history` / `conversationId` |
| 🗺️ 规划 | 工具调用扩展 | 更多 Tool 实现 |

---

## 十、项目文件清单

```
src/main/java/com/youkeda/exercise/claw/
├── ClawAssistantApplication.java     # Spring Boot 入口，编码检测
├── ClawException.java                # 统一业务异常
├── Main.java                         # CLI 主循环，命令分发
│
├── agent/
│   ├── AgentContext.java             # Agent 执行上下文
│   ├── AgentExecutor.java            # Agent 执行器接口
│   ├── ChatTool.java                 # 聊天 Tool（自动注册到 ToolRegistry）
│   ├── ImageGenerationTool.java      # 图片生成 Tool
│   ├── SimpleAgentExecutor.java      # 当前 Agent 实现（桥接 MessageRouter）
│   ├── Tool.java                     # 工具接口
│   ├── ToolRegistry.java             # 工具注册中心
│   └── VisionTool.java               # 视觉 Tool
│
├── ai/
│   ├── chat/ChatService.java         # 文本对话业务
│   ├── classifier/
│   │   ├── Intent.java               # CHAT / IMAGE_GENERATE / IMAGE_ANALYZE
│   │   ├── IntentClassifier.java     # 分类接口
│   │   └── LLMIntentClassifier.java  # LLM 分类实现
│   ├── image/
│   │   ├── ImageClient.java          # 图片生成 HTTP 客户端
│   │   ├── ImageGenerationService.java # 图片生成业务
│   │   └── ImageProperties.java      # 图片生成配置
│   ├── llm/
│   │   ├── LLMClient.java            # 文本 LLM 客户端
│   │   ├── LLMProperties.java        # LLM 配置
│   │   ├── SystemPromptProvider.java # 系统提示词加载
│   │   ├── VisionClient.java         # 视觉 LLM 客户端
│   │   └── VisionProperties.java     # 视觉模型配置
│   └── vision/VisionService.java     # 图片理解业务
│
├── command/
│   ├── CommandHandler.java           # 命令接口
│   ├── HelpCommand.java              # help
│   ├── StatusCommand.java            # status
│   └── VersionCommand.java           # version
│
├── common/HttpClientUtil.java        # HTTP GET 工具类
│
├── weather/
│   ├── WeatherCommand.java           # CLI 天气命令
│   ├── WeatherConfig.java            # 天气配置
│   ├── WeatherResponse.java          # 天气数据模型
│   └── WeatherTool.java              # 天气查询业务
│
└── wechat/
    ├── MessageRouter.java            # 消息路由器（意图路由）
    ├── client/WechatILinkClient.java # SDK 封装（登录/收发/下载）
    ├── config/WechatProperties.java  # 微信配置
    ├── handler/
    │   ├── MessageHandler.java       # 处理器接口
    │   ├── AIChatHandler.java        # 文本处理
    │   ├── ImageGenerationHandler.java # 图片生成处理
    │   ├── SimpleReplyHandler.java   # 兜底回复
    │   └── VisionHandler.java        # 图片分析处理
    ├── model/
    │   ├── MessageType.java          # 消息类型枚举
    │   └── WechatMessage.java        # 统一消息模型
    └── service/WechatMessageService.java # 轮询监听服务

src/main/resources/
├── application.properties            # 实际配置（不提交）
├── application.properties.example    # 配置模板
└── prompts/
    ├── system-prompt.txt             # LLM 系统提示词
    └── vision-system-prompt.txt      # 视觉模型系统提示词
```
