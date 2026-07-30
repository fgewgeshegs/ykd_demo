# Claw Assistant 架构文档

> 本文档描述项目的包结构、层次职责和依赖规则。
> 架构约束通过 `ArchitectureTest.java` 自动验证。

---

## 顶层包结构

```
com.youkeda.exercise.claw/
├── agent/           # Agent 内核（推理循环、Tool/Skill 调度、Memory、Plan）
├── ai/              # AI 客户端（llm / vision / image / voice / retrieval）
├── application/     # 应用入口（Spring Boot + CLI + command）
├── domain/          # 领域模型（19 个 POJO，零依赖）
├── feature/         # 业务功能模块（13 个模块，144 文件）
├── infrastructure/  # 基础设施（wechat / document / common）
├── notification/    # 通知框架（接口 + 事件模型）
├── skill/           # Skill 定义层（定义/注册/工作流框架）
└── tool/            # LLM 可调用工具（44 个，19 个子包）
```

---

## 各层职责

### `agent/` — Agent 内核

运行时核心，包含推理循环、Tool/Skill 调度、Memory、Plan 管理。

| 子包 | 职责 |
|---|---|
| `runtime/` | Tool 接口、ToolRegistry、ExecutionLoop、ToolExecutor、SkillExecutor 接口 |
| `skill/` | SkillRouter、Session 管理、Trigger 策略、执行调度 |
| `planning/` | PlanStore、PlanValidator |
| `memory/` | 短期/长期记忆、ContextStore |
| `activity/` | Agent 行为记录与 Dashboard 数据 |

**规则：** `agent/` 不依赖 `feature/`（通过 Tool/Skill 接口间接调用；`ScoutTriggerPolicy` 例外，因 trigger 路由需委托 feature 层做意图检测）。

### `tool/` — LLM 可调用工具

Agent 暴露给 LLM 的全部能力接口。每个工具实现 `agent.runtime.Tool` 接口，是 `feature/` 的薄 Facade。

- `tool/anime/` → 调用 `feature/anime/`
- `tool/weather/` → 调用 `feature/weather/`
- 等等（19 个子包，44 个工具）

**规则：** `tool/` 中的类必须 `implement Tool`。

### `skill/` — Skill 定义层

Skill 的抽象定义、注册和执行框架。平铺 16 个文件，不含具体业务逻辑。

| 文件 | 职责 |
|---|---|
| `SkillDefinition` / `WorkflowDefinition` | 定义描述 |
| `SkillRegistry` / `WorkflowRegistry` | 注册管理 |
| `WorkflowWorker` (interface) | 工作流执行器合约 |
| `InformationScoutSkillExecutor` | 具体 Skill 实现（仅编排，意图解析委托 `feature/scout/skill/`） |

**规则：** Skill 定义在 `skill/`，具体业务实现和意图解析放在对应的 `feature/{name}/` 下。

### `feature/` — 业务功能模块

Agent 暴露给用户的全部能力实现。13 个模块各对应一个产品功能：

| 模块 | 职责 | 子包特色 |
|---|---|---|
| `campus/` | 校园通知监控与推送 | `notification/`（5 个 Source）、`classifier/`、`policy/`、`store/` |
| `scout/` | 信息猎手——跨源信息收集与推荐 | `skill/`（intent resolve）、`collector/`、`judge/`、`store/` |
| `schedule/` | 课程表管理与提醒 | 26 文件，较密集 |
| `transport/` | 出行规划与打车 | `didi/` MCP 集成 |
| `travel/` | 旅行计划生成 | 状态化 Plan 管理 |
| `map/` | 地图搜索与路线规划 | 多 Provider 图片封装 |
| `weather/` | 天气查询与建议 | 含 `WeatherCommand` CLI |
| `anime/` | 动漫追番通知 | `notification/`（2 个 Source）、`client/`、`store/` |
| `file/` | 文件存储与管理 | `FileGenerationService`（AI 生成）、`FileService`（存储） |
| `websearch/` | 网页搜索 | 薄 Service 封装 |
| `task/` | 定时任务与计划 | `scheduler/`、`executor/` |
| `budget/` | 预算计算 | 10 个 DTO 类 |
| `holiday/` | 节假日查询 | 单文件 `HolidayDataLoader` |

### `domain/` — 领域模型

纯数据对象（POJO／record／enum），**零依赖**。

包含 19 个模型：`Anime`、`CampusConfig`、`TransportOption` 等。

### `infrastructure/` — 基础设施

技术实现细节，不含业务语义。

| 子包 | 职责 |
|---|---|
| `channel/wechat/` | 微信消息通道（路由、处理、登录、客户端） |
| `common/` | 通用工具类（HttpClientUtil、JacksonConfig、PromptLoader、**ClawException**） |
| `document/` | 文档解析（FileParseService，基于 Tika） |

### `ai/` — AI 客户端

AI 模型交互层，每个子包对应一个能力维度。

| 子包 | 职责 |
|---|---|
| `llm/` | LLM 客户端（ChatService 集成在此） |
| `vision/` | 视觉识别客户端 + Service |
| `image/` | 图片生成客户端 + Service |
| `voice/` | 语音客户端 + Service |
| `retrieval/` | 知识库 RAG 能力 |

### `notification/` — 通知框架

纯框架层：`NotificationSource` 接口定义通知契约，`NotificationEvent` 作为事件模型。
具体 Source 实现分布在各自的 `feature/{name}/notification/` 下（如 `campus/notification/`、`anime/notification/`）。

**规则：** `notification/` 自身不依赖任何业务层。

### `application/` — 应用入口

Spring Boot 启动类（`ClawAssistantApplication`）、CLI 入口（`Main`）、命令行命令（`command/`）。

**注意：** `ClawException` 统一放在 `infrastructure/common/` 下，不属于 `application/`。

---

## 依赖规则

```
application  ──→  agent, ai, feature, infrastructure, domain
     ↑
     │
agent ──→ tool, skill, domain, ai, infrastructure
  │          │
  ├──→ skill (路由+调度)
  ├──→ tool (能力调用)
  ├──→ ai (LLM 客户端)
  └──→ domain (使用领域模型)
  └──→ feature (仅 ScoutTriggerPolicy 例外)

tool ──→ agent/runtime (实现 Tool 接口)
     ──→ feature (调用业务实现)
     ──→ domain (使用领域模型)

feature ──→ infrastructure, domain, ai
     │
     └──→ notification (Source 实现在 feature/{name}/notification/ 下)

skill ──→ agent/runtime (SkillExecutor 接口)
     ──→ feature (具体业务实现，如 ScoutSubmissionService)

infrastructure ──→ domain (技术模型)
     │
     └──→ feature (非必须: 部分 handler 直连 feature 服务)
```

### 禁止规则

| 禁止 | 原因 |
|---|---|
| `domain` → 任何非自身包 | 领域模型必须纯净 |
| `agent` → `feature` | 内核不应直接依赖业务实现（ScoutTriggerPolicy 已标注例外） |
| `feature` → `agent` | 业务层不应引用内核内部 |

---

## 架构演进原则

1. **新功能** → 放在 `feature/{name}/` 下
2. **新 LLM 能力** → 在 `feature/` 中实现业务逻辑，在 `tool/` 中添加 Tool Facade
3. **新基础设施** → 放在 `infrastructure/` 下
4. **新通知类型** → 实现 `notification.NotificationSource`
5. **新 Skill** → 定义在 `skill/` 下，实现可引用 `feature/`
