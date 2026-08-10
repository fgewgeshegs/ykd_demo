# 🦀 Claw Assistant — 微信智能助手

> 一个运行在 **微信** 上的 ReAct Agent：通过 LLM tool-calling 自主调度 **40+ 个工具**，覆盖天气、地图、出行、旅行规划、课表、考试、文件、动漫追番、校园通知、信息猎手等场景。

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen)
![Maven](https://img.shields.io/badge/Maven-3.9-orange)
![WeChat](https://img.shields.io/badge/WeChat%20iLink-2.3.3-07c160)
![ArchUnit](https://img.shields.io/badge/Architecture-ArchUnit%20Verified-purple)

---

## 📌 项目简介

Claw Assistant 是一个以**微信消息为唯一入口**的智能助理机器人。用户通过微信直接发送文本、图片、语音、文件，Agent 内核会：

1. 解析用户意图（5 层 Skill 路由）
2. 自主规划并串行/并行调用多个工具
3. 综合结果生成自然语言回复，甚至主动推送通知

无需任何客户端 App，**打开微信就能用**——背后的 ReAct Agent + Function Calling 引擎负责一切。

---

## ✨ 功能亮点

### 1️⃣ 多模态消息理解
- **文本**：闲聊直答 + 意图自动触发工具
- **图片**：视觉识别，自动分析图片内容
- **语音**：ASR 识别 → 处理 → TTS 语音回复（MP3 直接发回微信）
- **文件**：PDF / Word / Excel 解析与智能总结

### 2️⃣ 40+ 工具自主调度
Agent 根据对话自动挑选工具、提取参数、串起多步流程：

| 领域 | 工具 |
|---|---|
| 🌤️ 天气 | 当前天气 + 14 天预报 |
| 🗺️ 地图 | 地点搜索 / 路线规划 / 距离比较 |
| 🚌 出行 | 大巴 / 自驾 / 高铁 / 飞机对比，滴滴打车 |
| 🧳 旅行 | 多轮信息收集 → 方案生成 → 预算核算 → 方案修订 |
| 📅 时间/节假日 | 日期推算、调休查询、团建适宜度评分 |
| 📚 课表/考试 | 课程导入、课表查询、考试提醒 |
| 📁 文件 | 增删改查、AI 生成 PDF/DOCX |
| 🎨 图像/语音 | AI 图片生成、文字转语音 |
| 🔍 搜索 | Tavily AI 搜索（其他工具无法满足时降级） |
| ⏰ 定时任务 | 创建 / 暂停 / 恢复 / 取消定时任务 |

### 3️⃣ Skill 智能路由
`SkillRouter` 采用 **5 层流水线**逐层裁决意图，只在必要时才把控制权交给 LLM：

```
L1 PendingInteraction → L2 ExplicitSwitch → L3 NewTrigger
→ L4 Continuation（会话超时/恢复） → L5 LLM Router
```

配合 Skill 白名单机制，每个 Skill 限定自己可用的工具集，降低误调用。

### 4️⃣ 主动通知（双通道）
- **校园通知**：定时监控校园公告，命中关键词即推送（5 个通知源）
- **动漫追番**：对接 AniList，开播自动提醒
- 支持 **微信 + QQ 邮箱** 双通道，`notification.preferred-channel` 可配置首选渠道，微信 token 失效自动降级邮箱

### 5️⃣ 长期记忆 + RAG 知识库
- 多轮对话持久化（SQLite），跨会话上下文
- Qdrant 向量库承载长期记忆 + Skill 专属 RAG 知识库
- 信息猎手（Scout）：跨源信息收集 → 相关度判定 → 定时推送

---

## 🏗️ 架构设计

### 分层架构（ArchUnit 强制校验）

```
根包 ClawAssistantApplication（Spring Boot 入口）
  └─→ agent（ReAct 内核）
         ├─→ tool（40+ LLM 工具，薄 Facade）──→ feature / agent.runtime / domain
         ├─→ skill（Skill 定义层）──→ feature / agent.runtime
         └─→ ai（LLM / Vision / Image / Voice / Retrieval）
  └─→ feature（13 个业务模块）──→ infrastructure / domain / ai / notification
  └─→ infrastructure（wechat 通道 / 文档解析 / 通用工具）
  └─→ domain（19 个纯 POJO，零依赖）
  └─→ notification（通知框架接口）
```

> 依赖方向单向向下，`ArchitectureTest`（ArchUnit）在 CI 中自动校验分层规则。

### ReAct 执行链路

```
微信消息 → WechatMessageService → MessageRouter → ChatHandler
  → ReActAgentExecutor
      ├─ 1. 取对话历史 + 当前消息（SQLite 持久化）
      ├─ 2. SkillRouter.route()：5 层流水线
      ├─ 3. 组装 System Prompt（基础提示词 + SKILL_CONTEXT + RAG 知识库）
      ├─ 4. 工具白名单 = 全局 ∪ 通用能力 ∪ 当前 Skill 可用工具
      ├─ 5. 闲聊快路径（无需工具 → 直接纯文本 LLM）
      └─ 6. ExecutionLoop tool-calling 循环（默认最多 15 轮）
             → ToolExecutor → Tool（LLM 按名调用，JSON Schema 传参）
```

### 关键设计点
- **跨轮状态管理**：旅行方案、课表待确认等由 `PendingToolCoordinator` 维护，支持多轮对话中逐步收集信息
- **双格式工具调用**：同时兼容 OpenAI `tool_calls` 与 DeepSeek DSML（`<invoke name=...>`）两种格式
- **二进制旁路**：图片/文件/语音等媒体通过「暂存-消费」模式绕过文本链路直接回传微信

---

## 🛠️ 技术栈

| 类别 | 技术 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.3.2（无 Web，纯消息驱动） |
| 构建 | Maven + Lombok |
| 消息通道 | 微信 iLink SDK 2.3.3 |
| LLM | DeepSeek（tool-calling / DSML） |
| 多模态 | DashScope：Qwen（视觉）、Paraformer（ASR）、CosyVoice（TTS） |
| 向量库 | Qdrant（长期记忆 + RAG） |
| Embedding | Ollama + BGE-M3 |
| 数据库 | SQLite（多实例分库：主库 / 课表 / 任务 / 文件） |
| 外部 API | Tavily（搜索）、腾讯地图、滴滴 MCP、WeatherAPI、Pexels、AniList |
| 文档解析 | Apache Tika + PDFBox + Apache POI |
| 测试 | JUnit 5 + Testcontainers（Qdrant）+ ArchUnit |

> **测试理念**：全部测试不使用 `@SpringBootTest`，单元测试直接 new 对象，轻量集成测试用真实 SQLite + `@TempDir`，快且稳定。

---

## 🎬 典型使用场景

| 场景 | 说明 |
|---|---|
| 💬 多轮对话 | 微信中连续追问，Agent 记住上下文 |
| 🛠️ 多工具协作 | 一次请求串起天气 + 地图 + 交通 + 预算 |
| 🧳 团建方案 | 从收集信息到方案生成、预算调整、导出 PDF 全流程 |
| 📢 主动通知 | 校园通知 / 动漫开播推送（微信 + 邮箱） |
| 🎨 媒体生成 | AI 图片、语音回复、PDF 导出 |

---

## 🚀 快速开始

### 环境要求
- JDK 21
- Maven 3.9+
- 本地服务：Qdrant（`docker run -p 6334:6334 qdrant/qdrant`）、Ollama（bge-m3 模型）

### 1. 配置

复制 `src/main/resources/application.properties.example` 为 `application.properties`，填入各项 API Key：

- `llm.*` — DeepSeek
- `vision.*` / `image.*` / `voice.*` — DashScope（Qwen / Paraformer / CosyVoice）
- `websearch` — Tavily
- `tencent` — 腾讯地图
- `wechat.ilink` — 微信机器人凭证

### 2. 构建

```bash
mvn clean compile        # 编译
mvn test                 # 全量测试（含架构校验）
mvn spring-boot:run      # 运行
```

### 3. 使用

微信扫码登录机器人后，直接发送消息即可。

---

## 📁 项目结构

```
claw-assistant/
├── src/main/java/com/youkeda/exercise/claw/
│   ├── agent/            # Agent 内核（ReAct 循环、Tool/Skill 调度、记忆）
│   ├── ai/               # LLM / Vision / Image / Voice / Retrieval 客户端
│   ├── domain/           # 19 个纯领域模型（零依赖）
│   ├── feature/          # 13 个业务模块（校园/动漫/课表/出行/旅行/地图/天气/文件…）
│   ├── infrastructure/   # 微信通道 / 文档解析 / 通用工具
│   ├── notification/     # 通知框架（接口 + 事件模型）
│   ├── skill/            # Skill 定义层（注册/路由/工作流框架）
│   └── tool/             # 40+ LLM 可调用工具（薄 Facade）
├── src/main/resources/
│   ├── config/           # skills.yml / skill-triggers.yml
│   └── prompts/          # 系统提示词与各 Skill 提示词
└── src/test/             # 单元测试 + 集成测试 + 架构测试
```

---

## ⚠️ 免责声明

本项目运行依赖多个外部服务与 API Key（微信 iLink、DeepSeek、DashScope、Qdrant、Ollama 等），**不提供内置免费服务**。`application.properties` 含真实密钥，已被 `.gitignore` 忽略，请勿提交。
