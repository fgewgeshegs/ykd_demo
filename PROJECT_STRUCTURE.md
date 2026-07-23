# Claw Assistant 项目框架结构文档

> **生成日期**: 2026-07-23  
> **基础框架**: Spring Boot 3.3.2 + Java 21  
> **项目路径**: `D:\ykd-project\test\ykd_demo`

---

## 一、项目概览

| 维度 | 说明 |
|------|------|
| **项目名称** | claw-assistant（Claw 助手） |
| **构建工具** | Maven |
| **核心定位** | WeChat iLink 智能助手，支持多模态交互（文字/语音/图片/文件）+ LLM Agent 工具调用 |
| **运行环境** | JVM 21（Windows 需加 `-Dfile.encoding=UTF-8`） |

### 技术栈依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot Starter | 3.3.2 | DI容器 + CLI生命周期 |
| Lombok | 1.18.34 | 代码简化 |
| Jackson | (Spring Boot 集成) | JSON 序列化 |
| wechat-ilink-sdk | 2.3.3 | 微信消息收发 |
| Spring Data Redis | (Spring Boot 集成) | 生产环境上下文存储 |
| Apache Tika Core | 2.9.2 | MIME 检测 + 文档解析 |
| Apache Tika Parsers Standard | 2.9.2 | PDF/DOCX/PPTX/XLSX 文本提取 |

### 外部服务集成

| 服务 | 用途 | 提供商 |
|------|------|--------|
| DeepSeek API | 文本 LLM | DeepSeek（OpenAI 兼容） |
| DashScope | 视觉理解 (qwen-vl-max) | 阿里云 |
| DashScope | 图片生成 (qwen-image-2.0) | 阿里云 |
| DashScope | 语音识别 (Paraformer) + 语音合成 (CosyVoice) | 阿里云 |
| 腾讯位置服务 | 地图搜索/路线/距离 | 腾讯 (lbs.qq.com) |
| Tavily | 网页搜索 | Tavily |
| WeatherAPI | 天气查询 | WeatherAPI.com |

---

## 二、顶层目录结构

```
D:\ykd-project\test\ykd_demo\
├── pom.xml                              # Maven 配置
├── CLAUDE.md                            # 项目开发指引
├── src/
│   ├── main/
│   │   ├── java/com/youkeda/exercise/claw/
│   │   │   ├── ClawAssistantApplication.java   # Spring Boot 入口
│   │   │   ├── ClawException.java              # 业务异常类
│   │   │   ├── Main.java                       # CLI 主控制器
│   │   │   │
│   │   │   ├── command/                        # CLI 命令层
│   │   │   │   ├── CommandHandler.java
│   │   │   │   ├── HelpCommand.java
│   │   │   │   ├── VersionCommand.java
│   │   │   │   └── StatusCommand.java
│   │   │   │
│   │   │   ├── common/                         # 公共工具
│   │   │   │   ├── HttpClientUtil.java
│   │   │   │   ├── JacksonConfig.java
│   │   │   │   └── PromptLoader.java
│   │   │   │
│   │   │   ├── agent/                          # Agent 体系（核心）
│   │   │   │   ├── AgentContext.java
│   │   │   │   ├── AgentExecutor.java
│   │   │   │   ├── SimpleAgentExecutor.java
│   │   │   │   ├── ReActAgentExecutor.java
│   │   │   │   ├── classify/
│   │   │   │   │   └── Intent.java
│   │   │   │   ├── memory/
│   │   │   │   │   ├── ContextStore.java
│   │   │   │   │   ├── Message.java
│   │   │   │   │   ├── InMemoryContextStore.java
│   │   │   │   │   ├── RedisContextStore.java
│   │   │   │   │   └── RedisContextProperties.java
│   │   │   │   └── tool/
│   │   │   │       ├── Tool.java
│   │   │   │       ├── ToolRegistry.java
│   │   │   │       ├── WechatMessageHandler.java
│   │   │   │       ├── LLMFunction.java
│   │   │   │       ├── LLMFunctionRegistry.java
│   │   │   │       ├── ChatTool.java
│   │   │   │       ├── VisionTool.java
│   │   │   │       ├── VoiceTool.java
│   │   │   │       ├── FileTool.java
│   │   │   │       ├── ImageGenerationTool.java
│   │   │   │       ├── FileGenerationTool.java
│   │   │   │       ├── SimpleReplyTool.java
│   │   │   │       └── TimeFunction.java
│   │   │   │
│   │   │   ├── ai/                             # AI 能力层
│   │   │   │   ├── chat/
│   │   │   │   │   └── ChatService.java
│   │   │   │   ├── vision/
│   │   │   │   │   └── VisionService.java
│   │   │   │   ├── image/
│   │   │   │   │   ├── ImageGenerationService.java
│   │   │   │   │   └── ImageClientException.java
│   │   │   │   ├── voice/
│   │   │   │   │   ├── VoiceService.java
│   │   │   │   │   └── VoiceClientException.java
│   │   │   │   ├── file/
│   │   │   │   │   ├── FileParseService.java
│   │   │   │   │   ├── FileGenerationService.java
│   │   │   │   │   └── FileParseProperties.java
│   │   │   │   └── llm/
│   │   │   │       ├── LLMClient.java
│   │   │   │       ├── LLMResponse.java
│   │   │   │       ├── ToolDefinition.java
│   │   │   │       ├── LLMProperties.java
│   │   │   │       ├── VisionClient.java
│   │   │   │       ├── VisionProperties.java
│   │   │   │       ├── ImageClient.java
│   │   │   │       ├── ImageProperties.java
│   │   │   │       ├── VoiceClient.java
│   │   │   │       └── VoiceProperties.java
│   │   │   │
│   │   │   ├── wechat/                         # 微信消息层
│   │   │   │   ├── MessageRouter.java
│   │   │   │   ├── client/
│   │   │   │   │   └── WechatILinkClient.java
│   │   │   │   ├── config/
│   │   │   │   │   └── WechatProperties.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── MessageType.java
│   │   │   │   │   ├── WechatMessage.java
│   │   │   │   │   └── WechatReply.java
│   │   │   │   └── service/
│   │   │   │       └── WechatMessageService.java
│   │   │   │
│   │   │   ├── weather/                        # 天气模块
│   │   │   │   ├── WeatherCommand.java
│   │   │   │   ├── WeatherFunction.java
│   │   │   │   ├── WeatherTool.java
│   │   │   │   ├── WeatherConfig.java
│   │   │   │   └── WeatherResponse.java
│   │   │   │
│   │   │   ├── map/                            # 地图模块
│   │   │   │   ├── MapService.java
│   │   │   │   ├── TencentMapClient.java
│   │   │   │   ├── TencentMapFunction.java
│   │   │   │   ├── TencentMapProperties.java
│   │   │   │   ├── TencentMapException.java
│   │   │   │   └── model/
│   │   │   │       ├── PlaceSearchRequest.java
│   │   │   │       ├── PlaceResponse.java
│   │   │   │       ├── RouteRequest.java
│   │   │   │       ├── RouteResponse.java
│   │   │   │       └── DistanceRequest.java
│   │   │   │
│   │   │   └── websearch/                      # 网页搜索模块
│   │   │       ├── SearchService.java
│   │   │       ├── WebSearchConfig.java
│   │   │       └── WebSearchFunction.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties          # 运行时配置（.gitignore'd）
│   │       ├── application.properties.example  # 配置模板
│   │       └── prompts/
│   │           ├── system-prompt.txt           # LLM 系统提示词
│   │           ├── vision-system-prompt.txt    # 视觉模型提示词
│   │           ├── image-system-prompt.txt     # 图片生成提示词
│   │           └── voice-system-prompt.txt     # 语音意图分类提示词
│   │
│   └── test/java/com/youkeda/exercise/claw/
│       └── agent/tool/
│           └── TimeFunctionTest.java
```

---

## 三、架构分层总览

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLI Subsystem                              │
│  Main.java ──▶ CommandHandler 注册 ──▶ Scanner 事件循环           │
│  (help / version / status / weather)                            │
├─────────────────────────────────────────────────────────────────┤
│                  WeChat iLink Subsystem                         │
│  WechatILinkClient ──▶ WechatMessageService ──▶ MessageRouter    │
│                           (轮询 getUpdates)       (路由分发)       │
├─────────────────────────────────────────────────────────────────┤
│                       Agent Layer                               │
│  ┌─────────────┐   ┌──────────────────┐   ┌──────────────────┐  │
│  │ Tool        │   │ LLMFunction      │   │ AgentExecutor    │  │
│  │ (Intent路由) │   │ (LLM ToolCalling)│   │ (执行调度)        │  │
│  └──────┬──────┘   └────────┬─────────┘   └────────┬─────────┘  │
│         │                   │                      │            │
│    ToolRegistry      LLMFunctionRegistry     ReActAgentExecutor │
│  (Intent→Tool)     (name→LLMFunction)        (tool-call loop)   │
│                                                      │           │
│                                            SimpleAgentExecutor  │
│                                           (MessageRouter桥接)   │
├─────────────────────────────────────────────────────────────────┤
│                        AI Service Layer                         │
│  ChatService / VisionService / VoiceService                     │
│  ImageGenerationService / FileParseService / FileGenerationService│
├─────────────────────────────────────────────────────────────────┤
│                        LLM Client Layer                         │
│  LLMClient (OpenAI兼容) / VisionClient / ImageClient / VoiceClient│
├─────────────────────────────────────────────────────────────────┤
│                     External Capabilities                       │
│  Weather | 腾讯地图 | Tavily搜索 | DeepSeek | DashScope          │
├─────────────────────────────────────────────────────────────────┤
│                       Memory Layer                              │
│  ContextStore ──▶ InMemoryContextStore / RedisContextStore       │
│                    (会话上下文持久化)                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、核心流程详述

### 4.1 应用启动流程

```
ClawAssistantApplication.main()
  │
  ├── SpringApplication.run()    # Spring Boot 启动
  │     ├── Main.init()           # CLI 注册所有 CommandHandler → 启动 Scanner 事件循环
  │     └── WechatILinkClient.init()  # 异步：SDK初始化→二维码登录→等待登录成功
  │
  └── WechatMessageService.start()  # @PostConstruct：等登录(60s)→启动轮询线程
```

### 4.2 微信消息处理全链路（核心）

```
WechatMessageService.pollLoop()
  │
  ├── wechatClient.receiveMessages()      # SDK getUpdates()
  │
  ├── buildWechatMessage(item)            # SDK MessageItem → WechatMessage
  │     ├── text_item    → TEXT
  │     ├── image_item   → IMAGE (CDN参数)
  │     ├── voice_item   → VOICE (CDN参数 + 服务端ASR文本)
  │     └── file_item    → FILE (CDN参数 + 文件名)
  │
  ├── saveMessageToContext(msg)           # 存上下文（write-through）
  │
  ├── wechatClient.startTyping(userId)   # 显示"正在输入"
  │
  └── messageRouter.route(msg)            # 消息路由分发
        │
        ├── TEXT → ChatTool.handle()
        │          └── ReActAgentExecutor.execute()
        │               └── LLM tool-calling 循环（最多10轮）
        │
        ├── IMAGE → VisionTool.handle()
        │            └── CDN下载 → VisionService → 多模态分析
        │
        ├── VOICE → VoiceTool ASR提取
        │            ├── 优先微信服务端ASR
        │            └── 降级：CDN下载 → Paraformer API
        │            → 文本走 ChatTool → 自动 TTS → 语音回复
        │
        ├── FILE → FileTool.handle()
        │           ├── CDN下载 → Tika MIME检测
        │           ├── image/* → base64 → VisionService 分析
        │           └── document/* → Tika文本提取 + 内嵌图片分析 → ChatService
        │
        └── 其他 → SimpleReplyTool.handle()  # 兜底
```

### 4.3 ReAct Agent 执行循环（核心创新）

```
ReActAgentExecutor.execute(context)
  │
  ├── 1. 获取对话历史（最近20条）+ 当前用户消息
  │
  ├── 2. 获取所有已注册 LLMFunction 的 ToolDefinition
  │
  └── 3. tool-calling 循环（最多10轮）
        │
        ├── LLMClient.chatWithTools(messages, tools)  # 发送含tools定义的请求
        │     │
        │     ├── finish_reason="stop" → 返回文本 → 结束
        │     │
        │     └── finish_reason="tool_calls" → 解析 List<ToolCall>
        │           │
        │           ├── Step 1: 逐个执行 tool call（通过 LLMFunctionRegistry 查找）
        │           │
        │           ├── Step 2: 合并所有 tool_calls 为1条 assistant 消息
        │           │    (支持单/多 tool_call，多 tool_call 合并为逗号分隔)
        │           │
        │           ├── Step 3: 每条 tool call 添加1条 tool 结果消息
        │           │
        │           └── 回到循环开头（继续让 LLM 基于结果生成回复/再次调用）
        │
        └── 达到最大轮次 → 返回超时提示
```

### 4.4 双轨制 Tool 体系

项目同时维护两套工具体系，分工明确：

| 维度 | Tool（旧） | LLMFunction（新） |
|------|-----------|-------------------|
| **注册中心** | `ToolRegistry` (Intent→Tool) | `LLMFunctionRegistry` (name→LLMFunction) |
| **调用方式** | 固定路由（MessageRouter 硬编码分发） | LLM 自主通过 Function Calling 决定 |
| **使用者** | SimpleAgentExecutor / MessageRouter | ReActAgentExecutor |
| **注册接口** | `Tool` (name/supportedIntents/execute) | `LLMFunction` (name/description/parameters/execute) |
| **演进方向** | 逐步退役 | 成为 Agent 主路径 |

**已注册的 LLMFunction 清单（8个）：**

| 函数名 | 实现类 | 功能 |
|--------|--------|------|
| `weather_query` | `WeatherFunction` | 查询城市天气 |
| `time_query` | `TimeFunction` | 时间/日期计算（3种操作） |
| `web_search` | `WebSearchFunction` | 互联网搜索 |
| `image_generate` | `ImageGenerationTool` | AI 图片生成 |
| `text_to_speech` | `VoiceTool` | 文本转语音 |
| `map_search_place` | `TencentMapFunction` | 地点搜索 |
| `map_route_planning` | `TencentMapFunction` | 路线规划 |
| `map_distance_calculate` | `TencentMapFunction` | 距离计算 |

---

## 五、关键模型与接口

### 5.1 领域模型

```
Message (record)
  ├── role: String            # user / assistant / tool
  ├── content: String         # 消息文本
  ├── mediaEncryptParam: String  # CDN 加密参数
  ├── mediaAesKey: String        # CDN 解密密钥
  ├── mediaUrl: String           # 媒体URL
  ├── toolCallId: String         # tool_call ID（逗号分隔支持多工具）
  └── toolName: String           # 函数名（逗号分隔支持多工具）

WechatMessage
  ├── userId: String
  ├── contextToken: String
  ├── type: MessageType       # TEXT / IMAGE / VOICE / FILE
  ├── text: String
  ├── (IMAGE fields): imageUrl, encryptQueryParam, aesKey
  ├── (VOICE fields): voiceText, playtime, encodeType, sampleRate, CDN params
  └── (FILE fields): fileName, fileMd5, fileLen, CDN params

WechatReply
  ├── type: MessageType       # TEXT / IMAGE / FILE
  ├── text: String
  ├── imageBytes: byte[]
  ├── fileBytes: byte[]
  ├── fileName: String
  └── fileDescription: String

LLMResponse
  ├── content: String
  ├── toolCalls: List<ToolCall>
  └── finishReason: String   # "stop" 或 "tool_calls"

ToolDefinition (record)
  ├── name: String
  ├── description: String
  └── parameters: JsonNode   # JSON Schema

AgentContext
  ├── userId / message / messageType / intent
  └── rawMessage: WechatMessage  # 原始消息（含CDN参数）
```

### 5.2 Intent 枚举

```java
CHAT           # 普通聊天
IMAGE_GENERATE # 图片生成
IMAGE_ANALYZE  # 图片分析
VOICE_REPLY    # 语音回复
FILE_GENERATE  # 文档生成（PDF/Word）
```

---

## 六、配置体系

### 6.1 配置前缀一览

| 前缀 | 所属模块 | 关键参数 |
|------|---------|---------|
| `llm.*` | 文本 LLM | apiKey, baseUrl, model（默认 deepseek-v4-flash） |
| `vision.*` | 视觉理解 | apiKey, baseUrl, model（qwen-vl-max） |
| `image.*` | 图片生成 | apiKey, baseUrl, model（qwen-image-2.0） |
| `voice.*` | 语音 ASR/TTS | apiKey, asr/tts端点, model, tts-enabled |
| `weather.api.*` | 天气 | key, url |
| `wechat.ilink.*` | 微信 | enabled, pollIntervalMs=3000 |
| `file.parse.*` | 文件解析 | maxFileSize=10MB, maxTextLength=30000, maxEmbeddedImages=5 |
| `websearch.api.*` | 网页搜索 | key, url, searchDepth, maxResults=5 |
| `tencent.key` | 腾讯地图 | API Key |

---

## 七、架构演进路线

```
SimpleAgentExecutor                    # 已完成：桥接 MessageRouter
    │  (IntentClassifier 硬编码路由)
    │
    ▼
IntentAgentExecutor                    # 规划中：Planner 替换 IntentClassifier
    │
    ▼
ReActAgentExecutor ←── 当前激活 ──→    # 已完成：LLM + Function Calling 自主循环
    (LLM 自主决定调用哪些工具)            (双轨并存：Tool 体系 + LLMFunction 体系)
```

**当前状态**：`MessageRouter` 中 TEXT 消息已通过 `ChatTool` → `ReActAgentExecutor` 走完整 tool-calling 循环；IMAGE/VOICE/FILE 仍走原有硬编码分发路径。`SimpleAgentExecutor` 保留作为降级/兼容桥接。

---

## 八、关键技术决策

| 决策 | 原因 |
|------|------|
| SDK 心跳关闭 (`heartbeatEnabled=false`) | 和 `getUpdates()` 共用锁，会抢锁丢消息 |
| 异步发送图片/文件 | 避免阻塞 WeChat 轮询线程 |
| 图片发送失败后文字降级 | 用户体验：至少知道发生了什么 |
| TTS 失败降级为文本回复 | 保证消息可达 |
| 优先用微信服务端 ASR | 更快、免费 |
| ContextStore write-through | 每个消息处理前先存上下文，保证历史完整 |
| 多 tool_call 合并为 1 条 assistant 消息 | 符合 OpenAI 并行函调规范 |
| 图片提示词扩写（CHAT<15字时） | 短描述 → 带历史上下文的完整图片描述，提升生图质量 |
| Tika MIME 检测基于 magic bytes | 比文件扩展名更可靠 |

---

## 九、模块交互关系图

```
                                  ┌─────────┐
                                  │  Main   │  CLI入口 (Scanner)
                                  └────┬────┘
                                       │ 注册/调用
                       ┌───────────────┼───────────────┐
                       ▼               ▼               ▼
                  HelpCommand   VersionCommand   StatusCommand

────────────────────────────────────────────────────────────────

              ┌─────────────────────────────┐
              │    WechatMessageService      │  后台轮询线程
              │  (pollLoop: 3s 间隔)         │
              └──────────┬──────────────────┘
                         │ getUpdates() / sendReply()
                         ▼
              ┌─────────────────────────────┐
              │      WechatILinkClient       │  SDK 封装
              │  (登录/收发/媒体下载/typing)  │
              └─────────────────────────────┘

              ┌─────────────────────────────┐
              │       MessageRouter          │  消息路由分发
              │  (按 MessageType 分发)        │
              └──────┬──────────────────────┘
                     │
        ┌────────────┼────────────┬────────────┐
        ▼            ▼            ▼            ▼
   ChatTool     VisionTool    VoiceTool     FileTool
   (TEXT)       (IMAGE)       (VOICE)       (FILE)
        │                         │
        ▼                         ▼
  ReActAgentExecutor        VoiceService
        │                  (ASR + TTS)
        ▼
   LLMClient.chatWithTools()
        │
        ▼
  LLMFunctionRegistry
   ├── weather_query
   ├── time_query
   ├── web_search
   ├── image_generate
   ├── text_to_speech
   ├── map_search_place
   ├── map_route_planning
   └── map_distance_calculate

              ┌─────────────────────────────┐
              │       ContextStore           │  会话记忆
              │  (InMemory / Redis)          │
              └─────────────────────────────┘
```

---

## 十、Prompt 文件管理

所有 LLM 提示词通过 `PromptLoader` 统一加载，支持类路径读取，有默认值兜底：

| 文件 | 使用者 | 用途 |
|------|--------|------|
| `prompts/system-prompt.txt` | `LLMClient` | 主 LLM 系统提示词 |
| `prompts/vision-system-prompt.txt` | `VisionClient` | 多模态视觉提示词 |
| `prompts/image-system-prompt.txt` | `ImageClient` | 图片生成提示词 |
| `prompts/voice-system-prompt.txt` | `VoiceService` | 语音意图分类提示词 |