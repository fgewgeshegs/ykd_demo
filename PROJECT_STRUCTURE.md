# Claw Assistant 项目结构分析

## 项目文件树

`
D:\ykd-project\test\
├── pom.xml                          # 根项目 POM（父模块）
├── .gitignore
│
└── ykd_demo/                        # 子模块：claw-assistant
    ├── pom.xml                      # 子模块 POM（Spring Boot 3.3.2 + Java 21）
    ├── MERGE-PROCESS.md             # 合并流程文档
    ├── .gitignore
    │
    └── src/
        ├── main/
        │   ├── java/com/youkeda/exercise/claw/
        │   │   ├── ClawAssistantApplication.java    # Spring Boot 启动类
        │   │   ├── ClawException.java                # 自定义异常
        │   │   ├── Main.java                         # CLI 主控制器
        │   │   │
        │   │   ├── agent/                            # Agent 核心层
        │   │   │   ├── AgentContext.java             # Agent 执行上下文
        │   │   │   ├── AgentExecutor.java            # Agent 执行器接口
        │   │   │   ├── ReActAgentExecutor.java       # ReAct 模式执行器
        │   │   │   │
        │   │   │   ├── memory/                       # 上下文记忆
        │   │   │   │   ├── ContextStore.java         # 会话存储接口
        │   │   │   │   ├── InMemoryContextStore.java # 内存实现
        │   │   │   │   ├── RedisContextStore.java    # Redis 实现
        │   │   │   │   ├── RedisContextProperties.java # Redis 配置
        │   │   │   │   └── Message.java              # 对话消息记录
        │   │   │   │
        │   │   │   └── tool/                         # 工具层（LLM Function Calling）
        │   │   │       ├── LLMFunction.java          # 工具接口
        │   │   │       ├── LLMFunctionRegistry.java  # 工具注册中心
        │   │   │       ├── FunctionExecutionContext.java # 工具执行上下文
        │   │   │       ├── ChatTool.java             # 聊天工具（TEXT 入口）
        │   │   │       ├── FileTool.java             # 文件消息处理器
        │   │   │       ├── FileGenerationTool.java   # 文件生成工具
        │   │   │       ├── ImageGenerationTool.java  # 图片生成工具
        │   │   │       ├── VisionTool.java           # 视觉识别工具
        │   │   │       ├── VoiceFunction.java        # 语音合成/识别工具
        │   │   │       ├── TimeFunction.java         # 时间日期工具
        │   │   │       ├── SimpleReplyTool.java      # 兜底回复处理器
        │   │   │       ├── PlanProposalFunction.java # 计划提议工具
        │   │   │       └── WechatMessageHandler.java # 微信消息处理器接口
        │   │   │
        │   │   ├── ai/                               # AI 能力层
        │   │   │   ├── chat/
        │   │   │   │   └── ChatService.java          # 聊天 AI 服务
        │   │   │   ├── file/
        │   │   │   │   ├── FileGenerationService.java # 文件生成服务（PDF/Word）
        │   │   │   │   ├── FileParseProperties.java   # 文件解析配置
        │   │   │   │   └── FileParseService.java      # 文件解析服务（Tika）
        │   │   │   ├── image/
        │   │   │   │   ├── ImageGenerationService.java # 图片生成服务
        │   │   │   │   └── ImageClientException.java  # 图片客户端异常
        │   │   │   ├── llm/                           # LLM 客户端层
        │   │   │   │   ├── LLMClient.java             # LLM 调用客户端
        │   │   │   │   ├── LLMProperties.java         # LLM 配置属性
        │   │   │   │   ├── LLMResponse.java           # LLM 响应模型
        │   │   │   │   ├── ToolDefinition.java        # 工具定义模型
        │   │   │   │   ├── DsmlToolCallParser.java    # DeepSeek tool_call 解析器
        │   │   │   │   ├── ImageClient.java           # 图片模型客户端
        │   │   │   │   ├── ImageProperties.java       # 图片模型配置
        │   │   │   │   ├── VisionClient.java          # 视觉模型客户端
        │   │   │   │   ├── VisionProperties.java      # 视觉模型配置
        │   │   │   │   ├── VoiceClient.java           # 语音模型客户端
        │   │   │   │   └── VoiceProperties.java       # 语音模型配置
        │   │   │   ├── vision/
        │   │   │   │   └── VisionService.java         # 视觉识别服务
        │   │   │   └── voice/
        │   │   │       ├── VoiceService.java          # 语音服务（ASR+TTS）
        │   │   │       └── VoiceClientException.java  # 语音客户端异常
        │   │   │
        │   │   ├── budget/                            # 预算核算模块
        │   │   │   ├── BudgetCalculatorFunction.java  # 预算核算工具
        │   │   │   ├── BudgetCalculatorService.java   # 预算核算服务
        │   │   │   ├── BatchPlanCostRequest.java      # 批量核算请求
        │   │   │   ├── PlanCostResult.java            # 方案成本结果
        │   │   │   ├── PlanCostOption.java            # 方案选项成本
        │   │   │   ├── PlanCostItem.java              # 费用项
        │   │   │   ├── CostBreakdown.java             # 费用明细分类
        │   │   │   ├── CostItemResult.java            # 单项费用结果
        │   │   │   ├── OptionCostResult.java          # 选项成本结果
        │   │   │   ├── PriceStatus.java               # 价格状态枚举
        │   │   │   └── PricingMode.java               # 计价模式枚举
        │   │   │
        │   │   ├── teamtrip/                          # 团建方案模块
        │   │   │   ├── TeamTripPlanFunction.java      # 团建方案工具
        │   │   │   ├── TeamTripPlanService.java       # 团建方案服务（核心）
        │   │   │   ├── TeamTripPlanDraft.java         # 团建方案草稿
        │   │   │   ├── TeamTripPlanOption.java        # 团建方案选项
        │   │   │   ├── TeamTripPlanStateStore.java    # 状态存储接口
        │   │   │   ├── InMemoryTeamTripPlanStateStore.java # 内存实现
        │   │   │   ├── RedisTeamTripPlanStateStore.java   # Redis 实现
        │   │   │   ├── TeamTripToolCallPolicy.java    # 工具调用策略
        │   │   │   ├── BudgetDecisionStatus.java      # 预算决策状态
        │   │   │   └── OptionDecisionStatus.java      # 选项决策状态
        │   │   │
        │   │   ├── transport/                         # 交通推荐模块
        │   │   │   ├── TransportRecommendFunction.java # 交通推荐工具
        │   │   │   ├── TransportService.java          # 交通推荐服务
        │   │   │   ├── TransportCalculator.java       # 交通费用计算器
        │   │   │   ├── TransportDecisionEngine.java   # 交通决策引擎
        │   │   │   └── model/
        │   │   │       ├── TransportOption.java       # 交通方式对比
        │   │   │       ├── TransportRequest.java      # 交通推荐请求
        │   │   │       └── TransportResult.java       # 交通推荐结果
        │   │   │
        │   │   ├── map/                               # 地图服务模块
        │   │   │   ├── TencentMapFunction.java        # 腾讯地图工具
        │   │   │   ├── MapService.java                # 地图服务层
        │   │   │   ├── TencentMapClient.java          # 腾讯地图 HTTP 客户端
        │   │   │   ├── TencentMapProperties.java      # 腾讯地图配置
        │   │   │   ├── TencentMapException.java       # 地图客户端异常
        │   │   │   └── model/
        │   │   │       ├── DistanceRequest.java       # 距离查询请求
        │   │   │       ├── PlaceSearchRequest.java    # 地点搜索请求
        │   │   │       ├── PlaceResponse.java         # 地点搜索响应
        │   │   │       ├── RouteRequest.java          # 路线规划请求
        │   │   │       └── RouteResponse.java         # 路线规划响应
        │   │   │
        │   │   ├── weather/                           # 天气查询模块
        │   │   │   ├── WeatherFunction.java           # 天气查询工具
        │   │   │   ├── WeatherTool.java               # 天气 API 调用层
        │   │   │   ├── WeatherConfig.java             # 天气 API 配置
        │   │   │   ├── WeatherResponse.java           # 天气响应模型
        │   │   │   └── WeatherCommand.java            # CLI weather 命令
        │   │   │
        │   │   ├── websearch/                         # 网页搜索模块
        │   │   │   ├── WebSearchFunction.java         # 网页搜索工具
        │   │   │   ├── SearchService.java             # Tavily 搜索服务
        │   │   │   └── WebSearchConfig.java           # Tavily API 配置
        │   │   │
        │   │   ├── holiday/                           # 节假日查询模块
        │   │   │   ├── HolidayCheckFunction.java      # 节假日查询工具
        │   │   │   ├── HolidayDataLoader.java         # 节假日数据加载器
        │   │   │   └── DayType.java                   # 日期类型枚举
        │   │   │
        │   │   ├── command/                           # CLI 命令模块
        │   │   │   ├── CommandHandler.java            # 命令处理器接口
        │   │   │   ├── HelpCommand.java               # help 命令
        │   │   │   ├── StatusCommand.java             # status 命令
        │   │   │   └── VersionCommand.java            # version 命令
        │   │   │
        │   │   ├── wechat/                            # 微信集成模块
        │   │   │   ├── MessageRouter.java             # 消息路由器
        │   │   │   ├── client/
        │   │   │   │   └── WechatILinkClient.java     # 微信 iLink SDK 封装
        │   │   │   ├── config/
        │   │   │   │   └── WechatProperties.java      # 微信配置属性
        │   │   │   ├── login/
        │   │   │   │   ├── LoginPageServer.java       # 登录状态可视化服务
        │   │   │   │   ├── LoginStateManager.java     # 登录状态管理器
        │   │   │   │   └── LoginStatus.java           # 登录状态枚举
        │   │   │   ├── model/
        │   │   │   │   ├── MessageType.java           # 消息类型枚举
        │   │   │   │   ├── WechatMessage.java         # 微信消息模型
        │   │   │   │   └── WechatReply.java           # 微信回复模型
        │   │   │   └── service/
        │   │   │       └── WechatMessageService.java  # 微信消息监听服务
        │   │   │
        │   │   └── common/                            # 公共工具模块
        │   │       ├── HttpClientUtil.java            # HTTP 客户端工具
        │   │       ├── JacksonConfig.java             # Jackson 配置
        │   │       └── PromptLoader.java              # Prompt 加载器
        │   │
        │   └── resources/
        │       ├── application.properties             # 主配置文件
        │       ├── application.properties.example     # 配置模板
        │       ├── prompts/
        │       │   ├── system-prompt.txt              # 系统 Prompt
        │       │   ├── image-system-prompt.txt        # 图片生成 Prompt
        │       │   ├── vision-system-prompt.txt       # 视觉识别 Prompt
        │       │   └── voice-system-prompt.txt        # 语音识别 Prompt
        │       └── holidays/
        │           ├── holidays-2025.json             # 2025 年节假日数据
        │           ├── holidays-2026.json             # 2026 年节假日数据
        │           └── holidays-2027.json             # 2027 年节假日数据
        │
        └── test/java/com/youkeda/exercise/claw/
            ├── agent/ReActAgentExecutorTest.java
            ├── agent/tool/TimeFunctionTest.java
            ├── ai/llm/DsmlToolCallParserTest.java
            ├── ai/llm/LLMClientReasoningContentTest.java
            ├── budget/BudgetCalculatorFunctionTest.java
            ├── budget/BudgetCalculatorServiceTest.java
            ├── holiday/HolidayCheckFunctionTest.java
            ├── teamtrip/TeamTripPlanServiceTest.java
            └── weather/WeatherFunctionTest.java
`

---

# 核心文件详细分析

---

## 一、启动类与入口

---

### 文件：ClawAssistantApplication.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/ClawAssistantApplication.java

**作用：**
Spring Boot 应用入口。自动扫描 com.youkeda.exercise.claw 包及其子包下的所有组件，启动整个应用。启动时检测 JVM 默认编码并给出中文乱码警告。

**所属模块：** 启动层（bootstrap）

**被谁调用：** 由 JVM 直接调用 main() 方法启动

**调用了谁：** SpringApplication.run() 启动 Spring 容器

**业务位置：** 用户启动应用时触发，是整个系统的入口点。

---

### 文件：Main.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/Main.java

**作用：**
CLI 主控制器。@PostConstruct 初始化时注册所有 CommandHandler 命令处理器，并在独立线程中启动控制台事件循环。负责接收用户输入、识别命令、分发执行，以及统一的异常捕获。

**所属模块：** 启动层 / CLI 交互层

**被谁调用：** Spring 容器 @PostConstruct 自动触发

**调用了谁：** CommandHandler 接口的所有实现（HelpCommand、StatusCommand、VersionCommand、WeatherCommand）

**业务位置：** 当用户通过控制台启动程序后，Main 负责提供 > 提示符并循环读取命令。对于微信模式，消息由 WechatMessageService 驱动，Main 仅作为 CLI 辅助入口。

---

### 文件：ClawException.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/ClawException.java

**作用：** 自定义业务异常基类。统一所有业务异常类型，便于在 CLI 层和微信消息处理层统一捕获展示错误信息。

**所属模块：** 公共异常层

**被谁调用：** 各 CommandHandler 实现、WeatherTool 等

**调用了谁：** 继承自 Exception

**业务位置：** 业务操作出现异常时抛给上层统一处理。

---

## 二、Agent 核心层

---

### 文件：AgentExecutor.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/AgentExecutor.java

**作用：** Agent 执行器接口。定义 execute(AgentContext) 契约，接收封装好的上下文，返回回复文本。

**所属模块：** agent

**被谁调用：** ChatTool.handle() 构建 AgentContext 后调用

**调用了谁：** 唯一实现 ReActAgentExecutor

**业务位置：** 整体流程中的执行桥接层 -- ChatTool 构建 AgentContext 后调用 execute()，进入 LLM tool-calling 循环。

---

### 文件：ReActAgentExecutor.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/ReActAgentExecutor.java

**作用：**
核心调度器。实现 ReAct（Reasoning + Acting）模式的 LLM tool-calling 循环：

1. 从 ContextStore 获取对话历史，构建消息列表
2. 快速路径检测：明显不需工具的闲聊直接 LLM 回复
3. 主循环：调用 LLM（携带工具定义）-> 判断返回结果
   - 返回文本 -> 保存并返回回复
   - 返回 tool_calls -> 逐个执行 LLMFunction -> 结果追加到消息列表 -> 继续下一轮
4. 达到最大轮次（12轮）或最大工具数（16次）-> 超时提示
5. 包含搜索频率限制、去重、阶段控制策略

**所属模块：** agent

**被谁调用：** AgentExecutor.execute() 被 ChatTool.handle() 调用

**调用了谁：**
- LLMClient.chatWithTools() -- LLM 请求
- LLMFunctionRegistry.getAllDefinitions() -- 获取所有工具定义
- LLMFunctionRegistry.find() -- 按名称查找工具
- ContextStore.getHistory() / ppend() -- 存取历史
- TeamTripPlanService.getDraft() / ecordToolResult() -- 团建流程集成
- TeamTripToolCallPolicy.validate() -- 工具调用策略校验

**业务位置：** 用户（微信/TEXT）-> MessageRouter -> ChatTool -> ReActAgentExecutor.execute() -> LLM tool-calling 循环 -> 返回回复。这是决定 LLM 如何理解用户意图、调用什么工具的决策中枢。

---

### 文件：AgentContext.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/AgentContext.java

**作用：** 封装一次 Agent 调用的所有输入信息：用户标识、消息文本、消息类型、原始微信消息、会话ID、历史消息等。

**所属模块：** agent

**被谁调用：** ChatTool.handle() 创建后传入 AgentExecutor.execute()

**调用了谁：** 引用 MessageType、WechatMessage

**业务位置：** 一次 Agent 执行的上下文载体。

---

### 文件：ContextStore.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/memory/ContextStore.java

**作用：** 会话上下文存储接口。定义 getHistory() 获取历史、ppend() 追加文本消息、ppendMessage() 追加带媒体参数的消息。

**所属模块：** agent/memory

**被谁调用：** ReActAgentExecutor、各类 Tool

**调用了谁：** InMemoryContextStore / RedisContextStore 实现

**业务位置：** 所有需要读写对话历史的地方，通过此接口完成持久化。

---

### 文件：InMemoryContextStore.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/memory/InMemoryContextStore.java

**作用：** ContextStore 的内存实现。使用 ConcurrentHashMap<String, Deque<Message>> 按用户存储最多 50 条消息，超限自动淘汰最早消息。默认启用（context.redis.enabled=false）。

**所属模块：** agent/memory

**被谁调用：** ContextStore 接口分发

**业务位置：** 非生产环境的默认上下文存储方案。

---

### 文件：RedisContextStore.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/memory/RedisContextStore.java

**作用：** ContextStore 的 Redis 实现。Key 格式 ctx:{userId}:msgs，LIST 数据结构，RPUSH 追加、LTRIM 限长、EXPIRE 设 TTL。

**所属模块：** agent/memory

**被谁调用：** ContextStore 接口分发（context.redis.enabled=true 时）

**调用了谁：** StringRedisTemplate（Spring Data Redis）

**业务位置：** 生产环境的上下文持久化方案。

---

### 文件：Message.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/memory/Message.java

**作用：** 单条对话消息记录（Java Record）。支持 user/ssistant/	ool 三种角色，可携带文本、媒体 CDN 参数、工具调用 ID、推理过程等。

**所属模块：** agent/memory

**被谁调用：** ContextStore 实现类、ReActAgentExecutor、LLMClient

**业务位置：** LLM 对话历史的原子单元。

---

## 三、工具层（Tool / Function Calling）

---

### 文件：LLMFunction.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/tool/LLMFunction.java

**作用：** 工具接口。定义 Agent 体系中所有 LLM 可调用的工具契约，包含 getName()、getDescription()、getParameters()（JSON Schema）、execute()。

**所属模块：** agent/tool

**被谁调用：** ReActAgentExecutor 根据 LLM 返回的 tool_calls 执行

**业务位置：** Agent 工具体系的根接口，所有业务能力统一实现此接口。

---

### 文件：LLMFunctionRegistry.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/tool/LLMFunctionRegistry.java

**作用：** 工具注册中心。ConcurrentHashMap 管理所有 LLMFunction 实例，提供 egister()、ind()、getAllDefinitions() 方法。

**所属模块：** agent/tool

**被谁调用：** 各 Tool 的 @PostConstruct 注册自身；ReActAgentExecutor 查找和获取定义

**调用了谁：** ToolDefinition 转换

**业务位置：** 工具注册 + 发现的中心节点。

---

### 文件：ChatTool.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/tool/ChatTool.java

**作用：** 聊天工具。所有 TEXT 消息的入口处理 Handler。构建 AgentContext，委托 ReActAgentExecutor 执行 LLM tool-calling 循环。特殊处理语音、文件、图片等暂存消费模式。

**所属模块：** agent/tool

**被谁调用：** MessageRouter.route() 在 TEXT 消息时调用

**调用了谁：** ReActAgentExecutor、VoiceFunction、FileGenerationTool

**业务位置：** 所有文本消息进入 Agent 处理的唯一入口。

---

### 文件：FileTool.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/tool/FileTool.java

**作用：** 文件消息处理器。处理 FILE 类型的微信消息，根据扩展名三路分发：图片 -> VisionService，文档 -> Tika 解析 + LLM 分析，不支持的格式 -> 提示。

**所属模块：** agent/tool

**被谁调用：** MessageRouter.route() 在 FILE 消息时调用

**调用了谁：** FileParseService、VisionService、ChatService

**业务位置：** 微信文件消息 -> MessageRouter -> FileTool。

---

### 文件：FileGenerationTool.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/tool/FileGenerationTool.java

**作用：** 文件生成工具。实现 LLMFunction，封装 FileGenerationService。LLM 调用 ile_generate 生成 PDF/Word。

**所属模块：** agent/tool

**被谁调用：** ReActAgentExecutor（LLMFunction）、MessageRouter（Handler）

**调用了谁：** FileGenerationService

**业务位置：** LLM 判断需要生成文件 -> 调用 ile_generate -> FileGenerationTool -> FileGenerationService。

---

### 文件：ImageGenerationTool.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/tool/ImageGenerationTool.java

**作用：** 图片生成工具。实现 LLMFunction，封装 ImageGenerationService + ImageClient。LLM 调用 image_generate 生成图片。

**所属模块：** agent/tool

**被谁调用：** ReActAgentExecutor（LLMFunction）、MessageRouter（Handler）

**调用了谁：** ImageGenerationService、ImageClient、LLMClient

**业务位置：** 用户要求画图 -> LLM 调用 image_generate -> 图片生成。

---

### 文件：VisionTool.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/tool/VisionTool.java

**作用：** 图片消息处理器。处理 IMAGE 类型微信消息，调用 VisionService 进行多模态分析。

**所属模块：** agent/tool

**被谁调用：** MessageRouter.route() 在 IMAGE 消息时调用

**调用了谁：** VisionService

**业务位置：** 微信图片消息 -> MessageRouter -> VisionTool。

---

### 文件：VoiceFunction.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/tool/VoiceFunction.java

**作用：** 语音功能工具。实现 LLMFunction + WechatMessageHandler：作为 Handler 处理 VOICE 消息（ASR 识别 -> 意图分类 -> ChatTool）；作为 Function 提供 	ext_to_speech 语音合成。

**所属模块：** agent/tool

**被谁调用：** MessageRouter（VOICE 消息）、ReActAgentExecutor（LLMFunction）

**调用了谁：** VoiceService、ChatTool、LLMClient

**业务位置：** 微信语音消息 -> ASR -> 意图分类 -> ChatTool 或忽略。

---

### 文件：TimeFunction.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/tool/TimeFunction.java

**作用：** 时间日期工具。处理相对日期换算（"下周六"、"3天后"），返回绝对日期。默认 Asia/Shanghai 时区。

**所属模块：** agent/tool

**被谁调用：** ReActAgentExecutor 作为 LLMFunction 调用

**调用了谁：** JDK java.time API

**业务位置：** 团建流程中用户给出相对日期 -> LLM 调用 	ime_query -> 返回绝对日期。

---

### 文件：PlanProposalFunction.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/agent/tool/PlanProposalFunction.java

**作用：** 计划提议工具。当 LLM 判断需要多工具协作时，调用此工具生成计划展示给用户确认。

**所属模块：** agent/tool

**被谁调用：** ReActAgentExecutor 作为 LLMFunction 调用

**业务位置：** 复杂请求 -> LLM 调用 plan_proposal -> 展示计划 -> 用户确认。

---

## 四、AI 能力层

---

### 文件：LLMClient.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/ai/llm/LLMClient.java

**作用：** LLM HTTP 客户端。封装大模型调用（兼容 OpenAI 协议），核心方法 chatWithTools()。支持 tool_calls 解析、DSML 格式解析（DeepSeek 特有）。

**所属模块：** ai/llm

**被谁调用：** ReActAgentExecutor 核心驱动

**调用了谁：** PromptLoader、LLMProperties、DsmlToolCallParser

**业务位置：** 所有 LLM 调用的底层 HTTP 通信层。

---

### 文件：ChatService.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/ai/chat/ChatService.java

**作用：** 聊天 AI 服务。提供纯文本对话能力（无工具调用），供 FileTool 在文件内容分析场景中使用。

**所属模块：** ai/chat

**被谁调用：** FileTool.handle()

**调用了谁：** LLMClient

**业务位置：** 文档解析后的内容分析。

---

### 文件：FileGenerationService.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/ai/file/FileGenerationService.java

**作用：** 文件生成服务。调用 LLM 生成内容 -> 组装为 PDF（PDFBox）或 Word（POI）文档。

**所属模块：** ai/file

**被谁调用：** FileGenerationTool

**调用了谁：** LLMClient、PDFBox/POI

**业务位置：** ile_generate -> FileGenerationService -> 生成 PDF/Word。

---

### 文件：FileParseService.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/ai/file/FileParseService.java

**作用：** 文件解析服务。底层使用 Apache Tika 检测格式和提取文本，提取文档中的嵌入图片。

**所属模块：** ai/file

**被谁调用：** FileTool.handle()

**调用了谁：** Apache Tika

**业务位置：** 用户发送文件 -> FileTool -> FileParseService -> 提取文本和图片。

---

### 文件：ImageGenerationService.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/ai/image/ImageGenerationService.java

**作用：** 图片生成服务。调用 ImageClient（通义万相 API）生成图片。

**所属模块：** ai/image

**被谁调用：** ImageGenerationTool

**调用了谁：** ImageClient

**业务位置：** image_generate -> 图片生成。

---

### 文件：VisionService.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/ai/vision/VisionService.java

**作用：** 视觉识别服务。调用 VisionClient（通义千问 VL）进行图片理解。

**所属模块：** ai/vision

**被谁调用：** VisionTool、FileTool

**调用了谁：** VisionClient

---

### 文件：VoiceService.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/ai/voice/VoiceService.java

**作用：** 语音服务。ASR（Paraformer）和 TTS（CosyVoice）。

**所属模块：** ai/voice

**被谁调用：** VoiceFunction

**调用了谁：** VoiceClient

---

## 五、业务模块

---

### 5.1 团建方案模块（teamtrip）

---

### 文件：TeamTripPlanService.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/teamtrip/TeamTripPlanService.java

**作用：** 团建方案核心服务（960行）。管理完整的团建方案状态机：需求收集（collect）-> 阶段推进 -> 方案保存（save_options）-> 选项选择（select_option）-> 预算决策（budget_decision）-> 修订（revise）。

**所属模块：** teamtrip

**被谁调用：** TeamTripPlanFunction.execute()

**调用了谁：** TeamTripPlanStateStore

**业务位置：**
用户在团建场景中 -> LLM 调用 	eam_trip_plan -> TeamTripPlanFunction -> TeamTripPlanService.handle() -> 逐步推进需求收集、并行查询、选项保存、用户选择、预算决策全流程。

---

### 文件：TeamTripPlanFunction.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/teamtrip/TeamTripPlanFunction.java

**作用：** 团建方案工具。函数名 	eam_trip_plan。解析 LLM JSON 参数，委托 TeamTripPlanService 处理。支持别名映射（origin -> departureCity 等）。

**所属模块：** teamtrip

**被谁调用：** ReActAgentExecutor 作为 LLMFunction 调用

**调用了谁：** TeamTripPlanService.handle()

---

### 文件：TeamTripToolCallPolicy.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/teamtrip/TeamTripToolCallPolicy.java

**作用：** 团建工具调用策略。约束工具调用的顺序和阶段，确保地图查询在节假日之后、交通推荐在天气之后、预算计算不能和搜索同批等。

**所属模块：** teamtrip

**被谁调用：** ReActAgentExecutor 每个 tool_call 前校验

**调用了谁：** TeamTripPlanStateStore 读取当前阶段

---

### 5.2 预算核算模块（budget）

---

### 文件：BudgetCalculatorService.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/budget/BudgetCalculatorService.java

**作用：** 预算核算服务。接收收费项目（人数/天数/费用项），计算总价、人均、分类明细，判断是否超预算。

**所属模块：** budget

**被谁调用：** BudgetCalculatorFunction

**业务位置：** LLM 调用 udget_calculator -> 核算费用。支撑团建流程中的成本核算。

---

### 5.3 交通推荐模块（transport）

---

### 文件：TransportService.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/transport/TransportService.java

**作用：** 交通推荐服务。核心链路：MapService.routePlanning() 获取距离 -> TransportCalculator 计算费用 -> TransportDecisionEngine 推荐。支持大巴/自驾/高铁/飞机四种方式。

**所属模块：** transport

**被谁调用：** TransportRecommendFunction

**调用了谁：** MapService、TransportCalculator、TransportDecisionEngine

---

### 文件：TransportCalculator.java

**作用：** 根据距离和人数计算各交通方式的费用、耗时、运力。

---

### 文件：TransportDecisionEngine.java

**作用：** 对各交通方式评分，给出推荐结论和推荐理由。

---

### 5.4 地图服务模块（map）

---

### 文件：TencentMapFunction.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/map/TencentMapFunction.java

**作用：** 腾讯地图工具。注册三个工具名：map_search_place（地点搜索）、map_route（路线规划）、map_distance（距离计算）。

**所属模块：** map

**被谁调用：** ReActAgentExecutor

**调用了谁：** MapService

---

### 文件：MapService.java / TencentMapClient.java / TencentMapProperties.java

**作用：** MapService 封装业务逻辑，TencentMapClient 封装腾讯地图 WebService API 的 HTTP 调用，TencentMapProperties 绑定 	encent.key 配置。

---

### 5.5 天气查询模块（weather）

---

### 文件：WeatherFunction.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/weather/WeatherFunction.java

**作用：** 天气查询工具。函数名 weather_query。超过 14 天返回 UNAVAILABLE。

**所属模块：** weather

**被谁调用：** ReActAgentExecutor

**调用了谁：** WeatherTool

---

### 文件：WeatherTool.java / WeatherConfig.java

**作用：** WeatherTool 调用 WeatherAPI.com，WeatherConfig 绑定 weather.api.* 配置。

---

### 5.6 网页搜索模块（websearch）

---

### 文件：WebSearchFunction.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/websearch/WebSearchFunction.java

**作用：** 网页搜索工具。函数名 web_search。

**所属模块：** websearch

**被谁调用：** ReActAgentExecutor

**调用了谁：** SearchService

---

### 文件：SearchService.java / WebSearchConfig.java

**作用：** SearchService 调用 Tavily API，返回 LLM 友好的结构化结果+AI 摘要。WebSearchConfig 绑定 websearch.api.* 配置。

---

### 5.7 节假日查询模块（holiday）

---

### 文件：HolidayCheckFunction.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/holiday/HolidayCheckFunction.java

**作用：** 节假日查询工具。函数名 holiday_check。查询日期类型（工作日/周末/节假日/调休），返回团建适宜性评分。

**所属模块：** holiday

**被谁调用：** ReActAgentExecutor

**调用了谁：** HolidayDataLoader

---

### 文件：HolidayDataLoader.java

**作用：** 启动时加载 2025-2027 年节假日 JSON 数据到内存。

---

### 5.8 微信集成模块（wechat）

---

### 文件：WechatILinkClient.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/wechat/client/WechatILinkClient.java

**作用：** 微信 iLink SDK 封装。管理登录生命周期（二维码展示、轮询登录状态）、消息收发、媒体文件上传下载。

**所属模块：** wechat

**被谁调用：** Spring 自动加载

**调用了谁：** ILinkClient（SDK）、LoginPageServer、LoginStateManager、WechatMessageService

**业务位置：** 微信集成起点。启动 -> 登录 -> 轮询消息 -> 转给 WechatMessageService。

---

### 文件：MessageRouter.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/wechat/MessageRouter.java

**作用：** 消息路由器。按类型分发：TEXT -> ChatTool、IMAGE -> VisionTool、VOICE -> VoiceFunction、FILE -> FileTool/FileGenerationTool、其他 -> SimpleReplyTool。

**所属模块：** wechat

**被谁调用：** WechatMessageService

**调用了谁：** 各类 WechatMessageHandler 实现

---

### 文件：WechatMessageService.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/wechat/service/WechatMessageService.java

**作用：** 微信消息监听服务。定时轮询新消息，构建 WechatMessage，委托 MessageRouter 分发，根据 WechatReply 类型调用 SDK 发送。

**所属模块：** wechat

---

## 六、CLI 命令模块

---

### 文件：CommandHandler.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/command/CommandHandler.java

**作用：** CLI 命令处理器接口。

### 文件：HelpCommand.java / StatusCommand.java / VersionCommand.java

**作用：** help（列出命令）、status（查看状态）、version（查看版本）。

---

## 七、公共工具模块

---

### 文件：HttpClientUtil.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/common/HttpClientUtil.java

**作用：** HTTP 客户端工具。封装 JDK HttpClient。

**被谁调用：** WeatherTool、TencentMapClient、SearchService 等

---

### 文件：JacksonConfig.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/common/JacksonConfig.java

**作用：** Jackson 全局配置（时区、日期格式、忽略未知属性等）。

---

### 文件：PromptLoader.java

**路径：**
ykd_demo/src/main/java/com/youkeda/exercise/claw/common/PromptLoader.java

**作用：** Prompt 文件加载器。从 classpath 加载 txt 提示词模板。

**被谁调用：** LLMClient

---

## 八、整体业务流程

### 完整调用链路

`
启动入口
  ClawAssistantApplication.main() -> Spring 容器初始化
    -> @ComponentScan 自动发现所有 Bean
    -> LLMFunctionRegistry 注册所有工具
    -> HolidayDataLoader 加载节假日数据
    -> WechatILinkClient 启动微信登录
    -> Main.startCli() 启动 CLI 线程

消息接收层
  微信: WechatILinkClient 轮询 -> WechatMessageService
  CLI:  Main 接收控制台输入

消息路由层
  MessageRouter.route()
    -> TEXT  -> ChatTool.handle()
    -> IMAGE -> VisionTool.handle()
    -> VOICE -> VoiceFunction.handle()
    -> FILE  -> FileTool.handle() / FileGenerationTool.handle()
    -> 其他  -> SimpleReplyTool.handle()

Agent 执行层（核心）
  ChatTool -> AgentContext -> ReActAgentExecutor.execute()
    -> 获取历史: ContextStore.getHistory()
    -> 快速通道: 简单闲聊直接 LLM 回复
    -> Tool-Calling 循环 (最多12轮)
      -> LLMClient.chatWithTools(messages, tools)
      -> LLM 返回文本 -> 结束，保存回复
      -> LLM 返回 tool_calls
        -> TeamTripToolCallPolicy.validate()（策略校验）
        -> LLMFunctionRegistry.find() -> execute()
          -> team_trip_plan -> TeamTripPlanFunction
          -> weather_query -> WeatherFunction
          -> holiday_check -> HolidayCheckFunction
          -> map_search_place / map_route / map_distance -> TencentMapFunction
          -> transport_recommend -> TransportRecommendFunction
          -> budget_calculator -> BudgetCalculatorFunction
          -> web_search -> WebSearchFunction
          -> time_query -> TimeFunction
          -> image_generate -> ImageGenerationTool
          -> file_generate -> FileGenerationTool
          -> text_to_speech -> VoiceFunction
          -> plan_proposal -> PlanProposalFunction
        -> 结果追加到消息列表 -> 继续循环

团建方案流程
  TeamTripPlanService.handle() 状态机
    Stage 0: NEED_MORE_INFORMATION -> 收集缺失字段
    Stage 1: READY_FOR_CONTEXT -> 并行查询
      -> holiday_check (节假日)
      -> map_search_place (目的地)
      -> weather_query (天气)
      -> transport_recommend (交通)
      -> web_search (补充搜索)
      -> budget_calculator (先估算)
    Stage 2: OPTIONS_READY_FOR_COSTING -> 保存方案选项
      -> budget_calculator (精确核算)
    Stage 3: AWAITING_OPTION_SELECTION -> 等待用户选择
    Stage 4: AWAITING_BUDGET_DECISION -> 超预算决策
    Stage 5: FINALIZABLE -> 最终方案可输出
    Stage R: REVISING -> 修订流程
`

### 工具分布总表

| 工具名称 | LLMFunction 实现 | 后端服务 | 用途 |
|---------|-----------------|---------|------|
| team_trip_plan | TeamTripPlanFunction | TeamTripPlanService | 团建方案需求收集和流程控制 |
| weather_query | WeatherFunction | WeatherTool + WeatherAPI.com | 天气查询 |
| holiday_check | HolidayCheckFunction | HolidayDataLoader | 节假日/调休查询 |
| map_search_place | TencentMapFunction | MapService + TencentMapClient | 地点搜索 |
| map_route | TencentMapFunction | MapService + TencentMapClient | 路线规划 |
| map_distance | TencentMapFunction | MapService + TencentMapClient | 距离计算 |
| transport_recommend | TransportRecommendFunction | TransportService + Calculator + DecisionEngine | 交通方式推荐 |
| budget_calculator | BudgetCalculatorFunction | BudgetCalculatorService | 方案费用核算 |
| web_search | WebSearchFunction | SearchService + Tavily API | 网页搜索 |
| time_query | TimeFunction | JDK java.time | 相对日期换算 |
| image_generate | ImageGenerationTool | ImageGenerationService + 通义万相 | 图片生成 |
| file_generate | FileGenerationTool | FileGenerationService | 文档生成（PDF/Word） |
| text_to_speech | VoiceFunction | VoiceService + 阿里云 CosyVoice | 语音合成 |
| plan_proposal | PlanProposalFunction | 无 | 计划提议与确认 |
