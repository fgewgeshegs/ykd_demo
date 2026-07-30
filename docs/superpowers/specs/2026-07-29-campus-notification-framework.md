# Campus 统一通知框架设计

> Claw Assistant — campus 模块重构与扩展
> 设计日期: 2026-07-29

---

## 1. 概述

### 1.1 实施阶段

本 spec 内容分两阶段实施：

| 阶段 | 内容 | 预计代码量 |
|------|------|-----------|
| **Phase 1** | 框架基础（Scheduler/Notifier/Source 接口 + DefaultPolicy）+ Exam 适配 + CompetitionSource | ~600 行 |
| **Phase 2** | ActivitySource + JobInfoSource + CourseReminderSource 占位 | ~500 行 |

第一阶段交付后即可上线运行，第二阶段不影响主流程。

### 1.2 背景

现有「考试提醒」模块（`com.youkeda.exercise.claw.campus`）实现了南邮教务处通知的自动检查与微信推送，涵盖采集 → 分类 → 策略决策 → 推送的完整链路。

本设计将考试提醒**重构为统一通知框架**，新增比赛、学校活动、就业资讯三类通知来源，并为未来课程表提醒（来自 `vae-tools` 分支的 `schedule` 模块）预留接入位置。

### 1.2 设计目标

- **非破坏性重构**：现有考试提醒功能不受影响，代码逐步迁移而非重写
- **来源可插拔**：新增通知类型只需实现 `NotificationSource` 接口，零改动已有 Source
- **共享基础设施**：去重、推送、待确认询问统一复用
- **纤薄 Source**：每个 Source 只做编排，不承载业务逻辑，20-30 行
- **独立配置**：每个 Source 可独立开关和设置推送偏好
- **可扩展**：未来课程提醒、GitHub 更新等均可作为新 Source 接入

### 1.3 范围

| 包含 | 不包含 |
|------|--------|
| 统一通知框架（Scheduler/Notifier/Source） | 课程表导入流程（`vae-tools` 已有） |
| 考试通知（现有代码适配） | PDF/Excel 课表解析（`vae-tools` 已有） |
| 比赛通知（新开发） | 学期计算体系（`vae-tools` 已有） |
| 学校活动通知（新开发） | Scout 信息猎手（独立子系统） |
| 就业资讯通知（新开发） | 定时任务通用调度器（可后续融合） |
| 课程提醒预留位置（不实现） | |

---

## 2. 系统架构

### 2.1 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                     CampusScheduler                          │
│  职责：决定「什么时候」触发                                   │
│  例：@Scheduled(cron="0 0 8 * * *") → 每天08:00执行          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      CampusNotifier                          │
│  职责：决定「哪些 Source」执行                                 │
│  for (NotificationSource s : sources) {                     │
│      if (s.supports(config)) s.check();                     │
│  }                                                          │
└──────┬──────────┬──────────┬──────────┬─────────────────────┘
       │          │          │          │
  ┌────▼───┐ ┌───▼────┐ ┌───▼────┐ ┌──▼──────────┐
  │ Exam   │ │Competi │ │Activity│ │ JobInfo     │
  │ Source │ │Source  │ │Source  │ │ Source      │
  │ (现有) │ │(新增)   │ │(新增)   │ │ (新增)       │
  └────┬───┘ └───┬────┘ └───┬────┘ └───┬─────────┘
       │         │          │          │
       └────┬────┴──────────┴──────────┘
            │
    ┌───────┴──────────────────────────────┐
    │        共享基础组件                    │
    ├──────────────────────────────────────┤
    │ CampusNotificationStore (去重)        │
    │ PendingAskStore (待确认)              │
    │ CampusConfigStore (全局配置)          │
    │ NotificationService (推送发送)        │
    │ CampusNotificationPolicy (决策框架)   │
    └──────────────────────────────────────┘
```

### 2.2 包结构

```
campus/
  ├── CampusNotifier.java              ← 统一编排入口
  ├── CampusScheduler.java             ← 定时调度（可多个）
  │
  ├── source/
  │   ├── NotificationSource.java      ← Source 接口
  │   ├── ExamSource.java              ← 考试通知入口
  │   ├── CompetitionSource.java       ← 比赛通知入口（新增）
  │   ├── ActivitySource.java          ← 学校活动通知入口（新增）
  │   ├── JobInfoSource.java           ← 就业资讯入口（新增）
  │   └── CourseReminderSource.java    ← 课程提醒占位（不实现）
  │
  ├── collector/                       ← Open: 各 Source 的收集器
  │   ├── NoticeCollector.java         ← 接口（可选）
  │   ├── ExamNoticeCollector.java     ← 现有，改名
  │   └── ...其他 Source 各自新增
  │
  ├── classifier/                      ← 保留现有
  │   ├── ExamRuleClassifier.java      ← 不变
  │   └── ExamLLMClassifier.java       ← 不变
  │
  ├── fetcher/                         ← 保留现有
  │   └── NoticeContentFetcher.java    ← 不变
  │
  ├── policy/
  │   ├── NotificationPolicy.java      ← 决策接口
  │   ├── DefaultPolicy.java           ← 通用决策流程
  │   └── rule/
  │       ├── PolicyRule.java          ← 规则提供者接口
  │       ├── ExamRules.java           ← 考试规则
  │       ├── CompetitionRules.java    ← 比赛规则（新增）
  │       ├── ActivityRules.java       ← 活动规则（新增）
  │       └── JobRules.java            ← 就业规则（新增）
  │
  ├── processor/
  │   └── CampusNoticeProcessor.java   ← 保留现有（仅考试使用）
  │
  ├── store/
  │   ├── CampusConfigStore.java       ← 保留现有
  │   ├── CampusNotificationStore.java ← 原 CampusNoticeStore（改名）
  │   └── PendingAskStore.java         ← 保留现有
  │
  ├── model/
  │   ├── NoticeType.java              ← 扩展枚举
  │   ├── NotificationItem.java        ← 通用通知事件模型（新增）
  │   ├── CampusConfig.java            ← 扩展 Source 开关
  │   └── ...其余保留
  │
  ├── monitor/
  │   └── CampusExamMonitor.java       ← 保留（ExamSource 适配期间兼容）
  │
  └── function/
      └── CampusReminderFunction.java  ← 扩展，统一配置入口
```

### 2.3 核心接口

#### NotificationSource

```java
public interface NotificationSource {
    /** Source 标识，用于日志和配置开关 */
    String getName();

    /** 判断此 Source 是否支持该配置（学校、用户类型等过滤） */
    boolean supports(CampusConfig globalConfig);

    /** 执行一次检查，Source 内部编排自己的组件 */
    void check();
}
```

#### PolicyRule（决策规则提供者）

```java
public interface PolicyRule {
    /** 返回该 Source 自动推送的类型列表 */
    List<String> getAutoPushTypes();

    /** 带上下文的判断：是否应该自动推送 */
    default boolean shouldAutoNotify(NotificationItem item) {
        return getAutoPushTypes().contains(item.getType());
    }
}
```

#### StorageItem（去重/存储的数据模型）

```java
public class NotificationItem {
    private Long id;
    private String source;         // EXAM / COMPETITION / ACTIVITY / JOB
    private String title;
    private String url;
    private String publishAt;
    private String content;
    private String type;           // 各 Source 内部的子类型
    private double confidence;
    private String scoreSource;
    private String classifierReason;
    private String status;         // UNPROCESSED / CLASSIFIED / IGNORED
    private Long processedAt;
    private Long createdAt;
}
```

---

## 3. 组件职责

### 3.1 CampusScheduler

```java
@Component
public class CampusScheduler {

    private final CampusNotifier notifier;
    private final CampusConfigStore configStore;

    /** 每天 08:00 执行各类通知检查 */
    @Scheduled(cron = "0 0 8 * * *")
    public void dailyCheck() {
        CampusConfig config = configStore.get();
        if (config == null || !config.isEnabled()) return;
        notifier.notifyAll(config);
    }

    /** 课程提醒频率不同，单独调度（预留，对应 vae-tools ScheduleReminderService） */
    @Scheduled(fixedRate = 60000)
    public void courseReminderCheck() {
        // 目前空实现，等 schedule 模块融合后启用
    }
}
```

**职责**：只决定「什么时候触发」，不关心「哪些 Source 执行」。一个 Scheduler 可以驱动所有 Source（如 dailyCheck），也可以单独为某个 Source 设置频率（如 courseReminderCheck 每分钟）。

### 3.2 CampusNotifier

```java
@Component
public class CampusNotifier {

    private final List<NotificationSource> sources;

    public CampusNotifier(List<NotificationSource> sources) {
        this.sources = sources;
    }

    public void notifyAll(CampusConfig config) {
        for (NotificationSource source : sources) {
            if (!source.supports(config)) continue;
            if (!config.isSourceEnabled(source.getName())) continue;
            log.info("通知检查启动 | source={}", source.getName());
            source.check();
        }
    }
}
```

**职责**：只决定「哪些 Source 执行」——通过 `supports()` 和 `isSourceEnabled()` 双层过滤。不包含任何业务逻辑。

### 3.3 ExamSource（重构现有）

```java
@Component
public class ExamSource implements NotificationSource {

    private final CampusNoticeCollector collector;
    private final CampusNoticeStore noticeStore;
    private final ExamRuleClassifier ruleClassifier;
    private final ExamLLMClassifier llmClassifier;
    private final DefaultPolicy policy;
    private final NotificationService notificationService;
    private final PendingAskStore pendingAskStore;

    @Override
    public String getName() { return "EXAM"; }

    @Override
    public boolean supports(CampusConfig config) {
        return config.getSchool() != null;
    }

    @Override
    public void check() {
        CampusConfig config = configStore.get();
        // 复用现有 CampusNoticeProcessor 的逻辑
        // 或者直接内联编排
        List<NoticeItem> notices = collector.collect(resolveUrl(config));
        List<NoticeItem> newNotices = noticeStore.deduplicate(notices);
        for (NoticeItem notice : newNotices) {
            ExamClassification result = classify(notice);
            Decision decision = policy.decide(result, config, new ExamRules());
            handleDecision(notice, result, decision);
        }
    }
}
```

**注意**：`ExamSource.check()` 内部逻辑就是现有 `CampusNoticeProcessor.process()` 的迁移。迁移过程中，`CampusExamMonitor` 和 `CampusNoticeProcessor` 并存，确保功能不中断。

### 3.4 CompetitionSource（新增）

#### 数据来源

采用多源策略：
1. **教务处通知** — 从现有采集结果中过滤出比赛相关通知（通过 `NoticeType` 扩展）
2. **赛氪/比赛信息网** — 按学校/类别搜索比赛
3. **用户自定义来源** — 后续通过对话配置

#### 分类规则

比赛分为两大类：
- **白名单比赛**（挑战杯、互联网+、大创等） → 自动推送
- **其他比赛** → 首次问用户是否关注 → 用户说关注以后自动推

#### 白名单管理

- 内置常见比赛白名单（学校级、国家级）
- 用户可通过对话添加/移除：「帮我关注美赛」
- 白名单存储在 Source 专属字段或 `campus_config.extra_config` 中

#### 示例 check()

```java
@Component
public class CompetitionSource implements NotificationSource {

    private final CompetitionCollector collector;
    private final CampusNotificationStore store;
    private final CompetitionClassifier classifier;
    private final DefaultPolicy policy;
    private final CompetitionConfigStore competitionConfig;

    @Override
    public String getName() { return "COMPETITION"; }

    @Override
    public boolean supports(CampusConfig config) {
        return config.getSchool() != null;
    }

    @Override
    public void check() {
        List<NotificationItem> items = collector.collect();
        List<NotificationItem> newItems = store.deduplicate(items);
        for (NotificationItem item : newItems) {
            String type = classifier.classify(item);
            Decision d = policy.decide(item, config,
                new CompetitionRules(competitionConfig.getWhitelist()));
            handleDecision(item, type, d);
        }
    }
}
```

### 3.5 ActivitySource（新增）

#### 数据来源

1. **学校团委/社团通知** — 特定页面爬取（如 NJUPT 团委通知页）
2. **教务处非考试通知** — 复用现有 Collector 的采集结果，用 `ActivityClassifier` 过滤
3. **公众号推送内容** — 后续扩展（需 RSS 或 API）

#### 分类与推送

| 类别 | 推送策略 |
|------|---------|
| 大型全校活动（校庆、晚会） | 自动推送 |
| 讲座/报告 | 问用户 |
| 社团活动 | 问用户 |
| 一般校园通知 | 忽略 |

### 3.6 JobInfoSource（新增）

#### 数据来源

1. **校园就业网** — 南邮就业信息网爬取
2. **应届生求职网/牛客** — 按学校/专业筛选
3. **宣讲会日历** — 从就业网获取

#### 分类与推送

| 类别 | 推送策略 |
|------|---------|
| 校园招聘会/双选会 | 自动推送 |
| 名企宣讲会 | 自动推送 |
| 实习招聘 | 问用户 |
| 就业指导讲座 | 问用户 |

#### 毕业生专有

`supports()` 方法中判断：如果用户未设置"毕业季"模式，则跳过就业推送。

### 3.7 CourseReminderSource（占位）

```java
@Component
public class CourseReminderSource implements NotificationSource {

    @Override
    public String getName() { return "COURSE"; }

    @Override
    public boolean supports(CampusConfig config) {
        return false; // 暂不启用，等待 vae-tools schedule 模块融合
    }

    @Override
    public void check() {
        // 空实现，融合 vae-tools 的 ScheduleReminderService 后填充
    }
}
```

**融合点标记**：`vae-tools` 的 `ScheduleReminderService` 目前独立使用 `CourseRepository`（纯 JDBC）和独立 DB 文件 `claw-schedule.db`。未来融合时：
1. 将 `ScheduleReminderService` 包装为 `CourseReminderSource`
2. `check()` 内部调用 `ScheduleReminderService.checkReminders()`
3. 推送方式改用共享的 `NotificationService`
4. 课程数据迁移到共享 SQLite 或保持独立（需评估）

---

## 4. 决策系统（Policy）

### 4.1 通用决策流程

所有 Source 共享同一决策流程，但**规则**按 Source 提供：

```
Decision decide(item, config, ruleProvider)

Step 1: IGNORE 检查
  非通知/未知类型 → IGNORE

Step 2: NOTIFY 检查
  ruleProvider.shouldAutoNotify(item) → NOTIFY
  （即 item.type 在 autoPushTypes 内）

Step 3: 历史回答检查
  PendingAskStore 查最新一次同类型的用户回答
  → 回答 yes → NOTIFY
  → 回答 no  → SKIP

Step 4: 首次询问
  → ASK（推送询问消息，等待用户回复）
```

### 4.2 DefaultPolicy 实现

```java
@Component
public class DefaultPolicy implements NotificationPolicy {

    private final PendingAskStore pendingAskStore;

    public Decision decide(NotificationItem item, CampusConfig config, PolicyRule rule) {
        // Step 1: IGNORE
        if ("IGNORE".equals(item.getType()) || "UNKNOWN".equals(item.getType())) {
            return Decision.IGNORE;
        }

        // Step 2: 自动推送
        if (rule.shouldAutoNotify(item)) {
            return Decision.NOTIFY;
        }

        // Step 3: 查历史
        Optional<String> answer = pendingAskStore.findLatestAnswer(item.getSource(), item.getType());
        if (answer.isPresent()) {
            return answer.get().equalsIgnoreCase("yes") ? Decision.NOTIFY : Decision.SKIP;
        }

        // Step 4: 首次询问
        return Decision.ASK;
    }
}
```

### 4.3 按 Source 的规则提供

```java
// 考试规则
public class ExamRules implements PolicyRule {
    @Override
    public List<String> getAutoPushTypes() {
        return List.of("FINAL_EXAM", "CET");
    }
}

// 比赛规则
public class CompetitionRules implements PolicyRule {
    private final List<String> whitelist;

    @Override
    public List<String> getAutoPushTypes() {
        return whitelist; // 白名单比赛直接推
    }
}

// 活动规则
public class ActivityRules implements PolicyRule {
    @Override
    public List<String> getAutoPushTypes() {
        return List.of("CAMPUS_EVENT"); // 大型活动自动推
    }
}

// 就业规则
public class JobRules implements PolicyRule {
    @Override
    public List<String> getAutoPushTypes() {
        return List.of("CAREER_FAIR", "ELITE_TALK"); // 招聘会、名企宣讲
    }
}
```

---

## 5. 数据模型与存储

### 5.1 模型类变更

#### NotificationItem（新增通用模型，替代 NoticeItem）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| source | String | 来源：EXAM / COMPETITION / ACTIVITY / JOB |
| title | String | 通知标题 |
| url | String | 原文链接（去重依据） |
| publishAt | String | 发布日期 |
| content | String | 正文（延迟加载） |
| type | String | 来源内部类型：FINAL_EXAM / CHALLENGE_CUP / CAMPUS_EVENT 等 |
| confidence | double | 分类置信度 |
| scoreSource | String | RULE / LLM / HYBRID / NONE |
| classifierReason | String | 分类理由 |
| status | String | UNPROCESSED / CLASSIFIED / IGNORED |
| processedAt | Long | 处理时间 |
| createdAt | Long | 创建时间 |

**NoticeItem 兼容**：新 `NotificationItem` 加 `source` 字段，现有 `NoticeItem` 保持不变或标记为 `@Deprecated`。

**ExamClassification → NotificationItem 桥接**：
- `DefaultPolicy.decide()` 接收 `NotificationItem` 作为统一参数
- 现有 `ExamRuleClassifier` / `ExamLLMClassifier` 产出 `ExamClassification`（保持不变）
- `ExamSource.check()` 内部自行桥接：`ExamClassification` → `NotificationItem.type` + `confidence`
- 新增 Source 的分类器直接产出 `NotificationItem`

#### NoticeType 枚举扩展

```java
public enum NoticeType {
    // 考试（已有）
    FINAL_EXAM, CET, RETAKE, MIDTERM, COMPUTER_LEVEL, PUTONGHUA,
    // 新增：考试类扩展
    OTHER_EXAM,
    // 比赛类
    CHALLENGE_CUP,        // 挑战杯
    INTERNET_PLUS,        // 互联网+
    INNOVATION_EXPO,      // 大创
    ACADEMIC_COMPETITION, // 学科竞赛
    SKILL_COMPETITION,    // 技能大赛
    OTHER_COMPETITION,    // 其他比赛
    // 活动类
    CAMPUS_EVENT,         // 全校大型活动
    LECTURE,              // 讲座/报告
    CLUB_ACTIVITY,        // 社团活动
    OTHER_ACTIVITY,       // 其他活动
    // 就业类
    CAREER_FAIR,          // 招聘会/双选会
    ELITE_TALK,           // 名企宣讲
    INTERN_RECRUIT,       // 实习招聘
    JOB_GUIDANCE,         // 就业指导
    OTHER_JOB,            // 其他就业
    // 通用
    NON_EXAM,             // 非通知（忽略）
    UNKNOWN
}
```

#### CampusConfig 扩展

```java
public class CampusConfig {
    // ... 现有字段不变
    private ExamPreferences preferences = new ExamPreferences();

    // 新增：各 Source 开关
    private Map<String, Boolean> sourceEnabled = new HashMap<>();

    public boolean isSourceEnabled(String sourceName) {
        return sourceEnabled.getOrDefault(sourceName, true);
    }

    public void setSourceEnabled(String sourceName, boolean enabled) {
        sourceEnabled.put(sourceName, enabled);
    }
}
```

### 5.2 数据库变更

#### campus_notice 表加 source 列

```sql
ALTER TABLE campus_notice ADD COLUMN source TEXT NOT NULL DEFAULT 'EXAM';
CREATE INDEX IF NOT EXISTS idx_notice_source ON campus_notice(source);
```

#### campus_config 的 extra_config 中存储 Source 开关

```json
{
  "autoPushTypes": ["FINAL_EXAM", "CET"],
  "remindBeforeDays": 1,
  "sourceEnabled": {
    "EXAM": true,
    "COMPETITION": true,
    "ACTIVITY": false,
    "JOB": false
  }
}
```

### 5.3 去重策略

统一使用 `url + source` 联合主键去重，而不是仅 url。同一 URL 可能既是考试通知又包含比赛信息（如教务处的一条通知同时告知期末安排和竞赛报名）。

---

## 6. 现有代码迁移计划

### 6.1 迁移原则

- **不中断现有功能**：考试提醒全程可用
- **分步迁移**：每步可单独提交和回滚
- **新旧并存**：迁移过程中 `CampusExamMonitor` 和 `ExamSource` 同时注册，逐步切换

### 6.2 迁移步骤

| 步骤 | 内容 | 改动文件数 | 风险 |
|------|------|-----------|------|
| 1 | 新增公共模型 `NotificationItem`、扩展 `NoticeType` | +1/-1 | 低 |
| 2 | `CampusNoticeStore` → `CampusNotificationStore` 改名 + 加 source 字段 | ~10 | 低 |
| 3 | 提取 `NotificationSource` 接口 + `CampusNotifier` | +2 | 低 |
| 4 | 提取 `DefaultPolicy` + `PolicyRule` | +3/-1 | 低 |
| 5 | 新增 `CompetitionSource`（基础版，仅白名单） | +3 | 低 |
| 6 | 新增 `ActivitySource`（基础版，仅大型活动） | +3 | 低 |
| 7 | 新增 `JobInfoSource`（基础版，仅招聘会） | +3 | 中 |
| 8 | 添加 `CourseReminderSource` 占位 | +1 | 极低 |
| 9 | 旧 `CampusExamMonitor` → `ExamSource` 迁移（并行运行后下线） | ~5 | 中 |

---

## 7. 各 Source 数据来源详细设计

### 7.1 ExamSource（已有，仅适配）

| 项目 | 值 |
|------|-----|
| URL | `https://jwc.njupt.edu.cn/1622/list34.psp` |
| 采集 | Jsoup 解析列表页 |
| 分类 | RuleClassifier → LLMClassifier 两级 |
| 频率 | 每天 08:00 |
| 白名单 | 期末、四六级自动推 |

### 7.2 CompetitionSource

| 项目 | 值 |
|------|-----|
| URL | 南邮教务处（复用 ExamSource 的采集结果） + 赛氪/竞赛信息网（待确认） |
| 采集 | Jsoup + HttpClient + WebSearch |
| 分类 | 规则匹配比赛名称 + LLM 增强 |
| 频率 | 每天 08:00 |
| 白名单 | 挑战杯、互联网+、大创、数学建模、ACM 等 |

> **注意**：赛氪等第三方竞赛平台的 URL 和解析逻辑需用户确认。第一版复用教务处采集结果，仅新增分类层。

**白名单配置示例**（存于 Source 专属配置）：

```json
{
  "whitelist": [
    "挑战杯", "互联网+", "大创", "数学建模",
    "ACM", "蓝桥杯", "计算机设计大赛"
  ],
  "ignoredTypes": [],
  "customKeywords": []
}
```

### 7.3 ActivitySource

| 项目 | 值 |
|------|-----|
| URL | 南邮团委 `https://tw.njupt.edu.cn`（待确认） + 校园通知（复用教务处采集） |
| 采集 | Jsoup 解析列表页 |
| 分类 | 规则（标题含"讲座""晚会""活动"）+ LLM |
| 频率 | 每天 08:00 |
| 默认策略 | 大型活动自动推，讲座/社团问用户 |

### 7.4 JobInfoSource

| 项目 | 值 |
|------|-----|
| URL | 南邮就业网（待确认）+ 91JOB（待确认） |
| 采集 | Jsoup 解析 |
| 分类 | 规则 + LLM |
| 频率 | 每天 08:00 |
| 默认策略 | 招聘会/名企宣讲自动推，实习/讲座问用户 |

---

## 8. 配置与用户交互

### 8.1 通用 LLM Function

现有 `ExamReminderFunction` 扩展为 `CampusReminderFunction`，支持：

| 用户说 | 行为 |
|--------|------|
| 「关注考试提醒」 | 设置考试 Source 开启 |
| 「帮我看看有什么比赛」 | 设置比赛 Source 开启 |
| 「不推比赛通知了」 | competition=false |
| 「加赛」 | 比赛白名单加"美赛" |
| 「我大四了」 | 开启就业推送 |
| 「就业太吵了」 | 关闭就业 |

### 8.2 默认开关

| Source | 默认值 | 理由 |
|--------|--------|------|
| EXAM | true | 核心功能 |
| COMPETITION | true | 大多数学生需要 |
| ACTIVITY | false | 噪音大，用户按需开启 |
| JOB | false | 仅毕业生需要 |

---

## 9. 边界情况与错误处理

| 场景 | 处理方式 |
|------|---------|
| 某 Source 配置的网站打不开 | 记录错误，仅跳过该 Source |
| 某 Source 检查异常 | 不传播到其他 Source，记录错误 |
| 用户没有设置学校 | 所有 Source 的 supports() 返回 false |
| 通知去重（URL + Source 联合） | 同一条通知不会被二次推送 |
| 用户关闭 Source | CampusNotifier 过滤掉 |
| 新学期课表接入 | CourseReminderSource 从 supports=false 改为 true |
| 比赛白名单为空 | 所有比赛都问用户，不自动推 |
| 毕业季结束后 | 用户手动关闭就业，或自动检测毕业时间 |

---

## 10. 后续可扩展

| 方向 | 改动量 | 说明 |
|------|--------|------|
| GitHub 更新通知 | 新 Source | 监控用户 Star/关注的仓库 |
| 动漫更新通知 | 新 Source | 根据用户追番列表检查更新 |
| 天气预警通知 | 新 Source | 极端天气自动推 |
| 新闻推送 | 新 Source | 可复用 Scout 的搜索结果 |
| 定时任务通用调度器 | 重构 Scheduler | 融合 vae-tools 的 TaskSchedulerService |
| 通知优先级 | Policy 增强 | 紧急通知（考试）始终推送，普通通知可合并 |

---

## 11. 文件清单及估算

### 新增文件

| 文件 | 估算行数 | 说明 |
|------|---------|------|
| `source/NotificationSource.java` | ~15 | 接口 |
| `source/ExamSource.java` | ~60 | 考试入口（编排现有组件） |
| `source/CompetitionSource.java` | ~60 | 比赛入口 |
| `source/ActivitySource.java` | ~60 | 活动入口 |
| `source/JobInfoSource.java` | ~60 | 就业入口 |
| `source/CourseReminderSource.java` | ~20 | 占位 |
| `collector/CompetitionCollector.java` | ~150 | 比赛采集 |
| `collector/ActivityCollector.java` | ~120 | 活动采集 |
| `collector/JobInfoCollector.java` | ~120 | 就业采集 |
| `classifier/CompetitionClassifier.java` | ~80 | 比赛分类 |
| `classifier/ActivityClassifier.java` | ~50 | 活动分类 |
| `classifier/JobClassifier.java` | ~50 | 就业分类 |
| `policy/NotificationPolicy.java` | ~10 | 接口 |
| `policy/DefaultPolicy.java` | ~45 | 通用决策 |
| `policy/rule/PolicyRule.java` | ~15 | 规则接口 |
| `policy/rule/ExamRules.java` | ~10 | 考试规则 |
| `policy/rule/CompetitionRules.java` | ~20 | 比赛规则 |
| `policy/rule/ActivityRules.java` | ~10 | 活动规则 |
| `policy/rule/JobRules.java` | ~10 | 就业规则 |
| `config/CompetitionConfigStore.java` | ~60 | 比赛白名单持久化 |
| **新增小计** | **~1025 行** | |

### 修改文件

| 文件 | 估算修改行 | 说明 |
|------|-----------|------|
| `model/NoticeType.java` | +~20 | 扩展枚举 |
| `model/CampusConfig.java` | +~15 | 加 sourceEnabled |
| `model/ExamPreferences.java` | +~5 | 兼容扩展 |
| `store/CampusNoticeStore.java` | 改名+~20 | 改为 CampusNotificationStore，加 source |
| `common/SqliteDatabaseInitializer.java` | +~5 | 加 source 列迁移 |
| `function/ExamReminderFunction.java` | +~20 | 扩展为 CampusReminderFunction |
| `CampusNotifier.java` | +~40 | 新文件 |
| `CampusScheduler.java` | +~30 | 新文件 |
| **修改小计** | **~155 行** | |

**总估算：~1180 行**

---

## 12. 不实现列表（明确排除）

1. **课程表导入** — `vae-tools` 已有完整实现，不重复开发
2. **课表可视化** — 同上
3. **考试日历自动生成** — 可从正文提取日期，但第一版不做
4. **通知中心/历史记录页面** — 纯微信推送，无需页面
5. **多学校多数据源配置中心** — 后续通过对话 LLMFunction 管理
6. **Source 动态注册/卸载** — 目前使用 Spring 的自动扫描
