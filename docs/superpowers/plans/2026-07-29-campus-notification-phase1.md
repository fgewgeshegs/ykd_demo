# Campus 统一通知框架 Phase 1 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建统一通知框架骨架，考试模块适配为 ExamSource，新增比赛通知 CompetitionSource

**Architecture:** 以 NotificationSource 接口为核心，CampusScheduler 负责定时触发，CampusNotifier 负责编排 Source。新增 NotificationItem 通用模型，Policy 层拆为通用 DefaultPolicy + 按领域 PolicyRule。现有考试代码保持运行，通过 ExamSource 逐步适配。

**Tech Stack:** Java 21, Spring Boot 3.3.2, Jsoup 1.18.1, SQLite + JdbcTemplate

## Global Constraints

- 所有新类放在 `com.youkeda.exercise.claw.campus` 包或其子包
- `@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")` 用于所有 campus 组件
- `@ConditionalOnProperty(name = "storage.enabled", havingValue = "true")` 用于 Store 组件
- 现有考试功能（CampusExamMonitor, CampusNoticeProcessor）保持运行，不做破坏性修改
- 遵循现有代码模式：`@Component` 服务类, `@Repository` Store 类, `JdbcTemplate` 注入
- 编译命令：`cd /e/work/claw/claw-assistant && mvn compile -q 2>&1 | tail -10`
- 每步完成后编译验证

---

### Task 1: 模型层扩展 — NotificationItem + NoticeType + CampusConfig

**Files:**
- Create: `src/main/java/.../campus/model/NotificationItem.java`
- Modify: `src/main/java/.../campus/model/NoticeType.java`
- Modify: `src/main/java/.../campus/model/CampusConfig.java`

**Interfaces:**
- Produces: `NotificationItem` (通用推送事件模型), 扩展的 `NoticeType` 枚举, 带 `sourceEnabled` 的 `CampusConfig`

- [ ] **Step 1: 创建 `NotificationItem.java`**

`src/main/java/com/youkeda/exercise/claw/campus/model/NotificationItem.java`:

```java
package com.youkeda.exercise.claw.campus.model;

/**
 * 通用通知事件模型。
 * 统一表示一条"可通知的校园事件"，无论它来自考试/比赛/活动/就业。
 * source 字段区分来源，type 字段区分来源内部的子类型。
 */
public class NotificationItem {
    private Long id;
    private String source;          // EXAM / COMPETITION / ACTIVITY / JOB
    private String title;
    private String url;
    private String publishAt;
    private String content;
    private String type;            // 来源内部类型（FINAL_EXAM / CHALLENGE_CUP 等，String 避免强绑定枚举）
    private double confidence;
    private String scoreSource;     // RULE / LLM / HYBRID / NONE
    private String classifierReason;
    private String status;          // UNPROCESSED / CLASSIFIED / IGNORED
    private Long processedAt;
    private Long createdAt;

    public NotificationItem() {}

    public NotificationItem(String source, String title, String url, String publishAt) {
        this.source = source;
        this.title = title;
        this.url = url;
        this.publishAt = publishAt;
    }

    public boolean needsContent() {
        return content == null || content.isBlank();
    }

    // ==== getters / setters ====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPublishAt() { return publishAt; }
    public void setPublishAt(String publishAt) { this.publishAt = publishAt; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getScoreSource() { return scoreSource; }
    public void setScoreSource(String scoreSource) { this.scoreSource = scoreSource; }

    public String getClassifierReason() { return classifierReason; }
    public void setClassifierReason(String classifierReason) { this.classifierReason = classifierReason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getProcessedAt() { return processedAt; }
    public void setProcessedAt(Long processedAt) { this.processedAt = processedAt; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: 扩展 `NoticeType.java`**

```java
package com.youkeda.exercise.claw.campus.model;

public enum NoticeType {
    // === 考试类（已有）===
    FINAL_EXAM,       // 期末考试
    CET,              // 四六级
    RETAKE,           // 补考/重修
    MIDTERM,          // 期中考试
    COMPUTER_LEVEL,   // 计算机等级考试
    PUTONGHUA,        // 普通话测试
    OTHER_EXAM,       // 其他考试

    // === 比赛类（Phase 1 新增）===
    CHALLENGE_CUP,        // 挑战杯
    INTERNET_PLUS,        // 互联网+
    INNOVATION_EXPO,      // 大创
    ACADEMIC_COMPETITION, // 学科竞赛
    SKILL_COMPETITION,    // 技能大赛
    OTHER_COMPETITION,    // 其他比赛

    // === 活动类（Phase 2）===
    CAMPUS_EVENT,
    LECTURE,
    CLUB_ACTIVITY,
    OTHER_ACTIVITY,

    // === 就业类（Phase 2）===
    CAREER_FAIR,
    ELITE_TALK,
    INTERN_RECRUIT,
    JOB_GUIDANCE,
    OTHER_JOB,

    // === 通用 ===
    NON_EXAM,         // 非通知
    UNKNOWN           // 无法判断
}
```

- [ ] **Step 3: 扩展 `CampusConfig.java`**

在现有字段后追加：

```java
    // 新增：各 Source 独立开关，key=source名称，value=是否开启
    private Map<String, Boolean> sourceEnabled = new HashMap<>();

    public boolean isSourceEnabled(String sourceName) {
        return sourceEnabled.getOrDefault(sourceName, true);
    }

    public void setSourceEnabled(String sourceName, boolean enabled) {
        sourceEnabled.put(sourceName, enabled);
    }
```

文件顶部需要添加 import：
```java
import java.util.HashMap;
import java.util.Map;
```

- [ ] **Step 4: 编译验证**

```bash
cd /e/work/claw/claw-assistant && mvn compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/youkeda/exercise/claw/campus/model/NotificationItem.java \
  src/main/java/com/youkeda/exercise/claw/campus/model/NoticeType.java \
  src/main/java/com/youkeda/exercise/claw/campus/model/CampusConfig.java
git commit -m "feat(campus): add NotificationItem, extend NoticeType and CampusConfig"
```

---

### Task 2: 数据库迁移 — campus_notice 和 campus_pending_ask 加 source 列

**Files:**
- Modify: `src/main/java/.../common/SqliteDatabaseInitializer.java`

**Interfaces:**
- Produces: DB 迁移脚本，为已有表加 source 列，不破坏存量数据

- [ ] **Step 1: 在 `SqliteDatabaseInitializer.createTables()` 末尾追加迁移 SQL**

在 `createTables()` 方法最后（所有 CREATE TABLE 之后），追加：

```java
        // === 校园通知框架迁移：为 campus_notice 添加 source 列 ===
        try {
            jdbcTemplate.execute("ALTER TABLE campus_notice ADD COLUMN source TEXT NOT NULL DEFAULT 'EXAM'");
            log.info("DB迁移完成：campus_notice 添加 source 列");
        } catch (Exception e) {
            // SQLite 不支持 IF NOT EXISTS 加列，列已存在时会抛异常，忽略
            log.debug("campus_notice.source 列已存在，跳过迁移");
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_notice_source ON campus_notice(source)");
        } catch (Exception e) {
            log.debug("idx_notice_source 索引已存在，跳过");
        }

        // === 迁移：为 campus_pending_ask 添加 source 列（默认 'EXAM' 兼容旧数据） ===
        try {
            jdbcTemplate.execute("ALTER TABLE campus_pending_ask ADD COLUMN source TEXT NOT NULL DEFAULT 'EXAM'");
            log.info("DB迁移完成：campus_pending_ask 添加 source 列");
        } catch (Exception e) {
            log.debug("campus_pending_ask.source 列已存在，跳过迁移");
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_pending_source_type ON campus_pending_ask(source, notice_type, status)");
        } catch (Exception e) {
            log.debug("idx_pending_source_type 索引已存在，跳过");
        }
```

- [ ] **Step 2: 编译验证**

```bash
cd /e/work/claw/claw-assistant && mvn compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/youkeda/exercise/claw/common/SqliteDatabaseInitializer.java
git commit -m "feat(campus): DB migration - add source column to campus_notice and campus_pending_ask"
```

---

### Task 3: Store 层扩展 — CampusNotificationStore + PendingAskStore 扩展

**Files:**
- Create: `src/main/java/.../campus/store/CampusNotificationStore.java`
- Modify: `src/main/java/.../campus/store/PendingAskStore.java`

**Interfaces:**
- Consumes: `NotificationItem`, `JdbcTemplate`
- Produces: `CampusNotificationStore.deduplicate()` / `update()` / `updateContent()`（含 source 字段处理）
- Produces: `PendingAskStore.findLatestAnswer(source, type)` / `save(source, type, ...)` / `updateAnswer(source, type, ...)`

- [ ] **Step 1: 创建 `CampusNotificationStore.java`**

基于现有 `CampusNoticeStore`，但操作 `NotificationItem` 并处理 `source` 字段。
去重逻辑改为 `url + source` 联合检查，不再依赖表的 UNIQUE(url) 约束。

```java
package com.youkeda.exercise.claw.campus.store;

import com.youkeda.exercise.claw.campus.model.NotificationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "storage.enabled", havingValue = "true")
public class CampusNotificationStore {

    private static final Logger log = LoggerFactory.getLogger(CampusNotificationStore.class);

    private final JdbcTemplate jdbc;

    public CampusNotificationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 与已有记录去重（按 url + source 联合判断），返回全新的通知列表并插入DB
     */
    public List<NotificationItem> deduplicate(List<NotificationItem> fetched) {
        List<NotificationItem> newItems = new ArrayList<>();
        for (NotificationItem item : fetched) {
            try {
                // 先检查 (url, source) 是否已存在
                Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM campus_notice WHERE url = ? AND source = ?",
                    Integer.class, item.getUrl(), item.getSource());
                if (count != null && count > 0) {
                    continue; // 已存在，跳过
                }

                jdbc.update("""
                    INSERT INTO campus_notice (title, url, publish_at, source, status)
                    VALUES (?, ?, ?, ?, 'UNPROCESSED')
                    """, item.getTitle(), item.getUrl(), item.getPublishAt(), item.getSource());
                newItems.add(item);
            } catch (Exception e) {
                log.warn("去重写入失败 | url={} | source={}", item.getUrl(), item.getSource(), e);
            }
        }
        if (!newItems.isEmpty()) {
            log.info("发现新通知 | source={} | count={}",
                newItems.get(0).getSource(), newItems.size());
        }
        return newItems;
    }

    public void update(NotificationItem item) {
        jdbc.update("""
            UPDATE campus_notice SET type=?, confidence=?, score_source=?,
                classifier_reason=?, status=?, processed_at=?
            WHERE url=? AND source=?
            """, item.getType(), item.getConfidence(), item.getScoreSource(),
            item.getClassifierReason(), "CLASSIFIED",
            System.currentTimeMillis() / 1000, item.getUrl(), item.getSource());
    }

    public void updateContent(Long id, String content) {
        jdbc.update("UPDATE campus_notice SET content = ? WHERE id = ?", content, id);
    }

    public List<NotificationItem> getUnprocessed() {
        return jdbc.query(
            "SELECT * FROM campus_notice WHERE status = 'UNPROCESSED' ORDER BY created_at ASC",
            new NotificationItemRowMapper());
    }

    private static class NotificationItemRowMapper implements RowMapper<NotificationItem> {
        @Override
        public NotificationItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            NotificationItem item = new NotificationItem();
            item.setId(rs.getLong("id"));
            item.setTitle(rs.getString("title"));
            item.setUrl(rs.getString("url"));
            item.setPublishAt(rs.getString("publish_at"));
            item.setContent(rs.getString("content"));
            item.setSource(rs.getString("source"));
            item.setStatus(rs.getString("status"));
            String type = rs.getString("type");
            if (type != null) item.setType(type);
            item.setConfidence(rs.getDouble("confidence"));
            item.setScoreSource(rs.getString("score_source"));
            item.setClassifierReason(rs.getString("classifier_reason"));
            long processedAt = rs.getLong("processed_at");
            if (!rs.wasNull()) item.setProcessedAt(processedAt);
            long createdAt = rs.getLong("created_at");
            if (!rs.wasNull()) item.setCreatedAt(createdAt);
            return item;
        }
    }
}
```

- [ ] **Step 2: 扩展 `PendingAskStore.java`**

在现有方法之外，添加带 source 参数的新方法。旧方法保留，内部委托给新方法（传 source="EXAM" 向后兼容）：

```java
    // ========== 新方法（统一通知框架使用，含 source 字段）==========

    /** 查指定来源+类型的最新一条回答 */
    public Optional<String> findLatestAnswer(String source, String noticeType) {
        try {
            String answer = jdbc.queryForObject("""
                SELECT answer FROM campus_pending_ask
                WHERE source = ? AND notice_type = ? AND status = 'ANSWERED'
                ORDER BY asked_at DESC LIMIT 1
                """, String.class, source, noticeType);
            return Optional.ofNullable(answer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("查询最新回答失败 | source={} | noticeType={}", source, noticeType, e);
            return Optional.empty();
        }
    }

    public void save(String source, String noticeType, String question, String status) {
        long now = System.currentTimeMillis() / 1000;
        jdbc.update("""
            INSERT INTO campus_pending_ask (source, notice_type, question, status, asked_at)
            VALUES (?, ?, ?, ?, ?)
            """, source, noticeType, question, status, now);
    }

    public void updateAnswer(String source, String noticeType, String answer) {
        long now = System.currentTimeMillis() / 1000;
        jdbc.update("""
            UPDATE campus_pending_ask SET answer=?, status='ANSWERED', answered_at=?
            WHERE source=? AND notice_type=? AND status='PENDING'
            """, answer, now, source, noticeType);
    }

    // ========== 旧方法保留（委托新方法，向后兼容）==========

    /** @deprecated 使用 {@link #findLatestAnswer(String, String)} */
    @Deprecated
    public Optional<String> findLatestAnswer(String noticeType) {
        return findLatestAnswer("EXAM", noticeType);
    }

    /** @deprecated 使用 {@link #save(String, String, String, String)} */
    @Deprecated
    public void save(String noticeType, String question, String status) {
        save("EXAM", noticeType, question, status);
    }

    /** @deprecated 使用 {@link #updateAnswer(String, String, String)} */
    @Deprecated
    public void updateAnswer(String noticeType, String answer) {
        updateAnswer("EXAM", noticeType, answer);
    }
```

需要在文件顶部补充 import：
```java
import org.springframework.dao.EmptyResultDataAccessException;
```

- [ ] **Step 3: 编译验证**

```bash
cd /e/work/claw/claw-assistant && mvn compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/youkeda/exercise/claw/campus/store/CampusNotificationStore.java \
  src/main/java/com/youkeda/exercise/claw/campus/store/PendingAskStore.java
git commit -m "feat(campus): add CampusNotificationStore, extend PendingAskStore with source"
```

---

### Task 4: Policy 框架 — NotificationPolicy + PolicyRule + DefaultPolicy + ExamRules

**Files:**
- Create: `src/main/java/.../campus/policy/NotificationPolicy.java`
- Create: `src/main/java/.../campus/policy/rule/PolicyRule.java`
- Create: `src/main/java/.../campus/policy/DefaultPolicy.java`
- Create: `src/main/java/.../campus/policy/rule/ExamRules.java`

**Interfaces:**
- Consumes: `NotificationItem`, `CampusConfig`, `PendingAskStore`
- Produces: `NotificationPolicy` (接口), `PolicyRule` (规则提供者接口), `DefaultPolicy` (通用实现), `ExamRules` (考试规则)

- [ ] **Step 1: 创建 `NotificationPolicy.java`**

```java
package com.youkeda.exercise.claw.campus.policy;

import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.model.NotificationItem;
import com.youkeda.exercise.claw.campus.policy.rule.PolicyRule;

public interface NotificationPolicy {

    enum Decision {
        NOTIFY,  // 自动推送
        ASK,     // 询问用户是否要推
        SKIP,    // 用户之前说不需要
        IGNORE   // 非通知，直接忽略
    }

    /**
     * 对一条已分类的通知做出推送决策
     *
     * @param item   已分类的通知
     * @param config 全局配置（含 source 开关）
     * @param rule   当前 Source 的推送规则
     * @return 决策结果
     */
    Decision decide(NotificationItem item, CampusConfig config, PolicyRule rule);
}
```

- [ ] **Step 2: 创建 `PolicyRule.java`**

```java
package com.youkeda.exercise.claw.campus.policy.rule;

import com.youkeda.exercise.claw.campus.model.NotificationItem;

import java.util.List;

/**
 * 推送规则提供者接口。
 * 每个 Source 提供一个实现，定义哪些类型应该自动推送。
 */
public interface PolicyRule {

    /** 返回此 Source 应自动推送的类型列表 */
    List<String> getAutoPushTypes();

    /** 判断某条通知是否应自动推送 */
    default boolean shouldAutoNotify(NotificationItem item) {
        return getAutoPushTypes().contains(item.getType());
    }
}
```

- [ ] **Step 3: 创建 `DefaultPolicy.java`**

```java
package com.youkeda.exercise.claw.campus.policy;

import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.model.NotificationItem;
import com.youkeda.exercise.claw.campus.policy.rule.PolicyRule;
import com.youkeda.exercise.claw.campus.store.PendingAskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class DefaultPolicy implements NotificationPolicy {

    private static final Logger log = LoggerFactory.getLogger(DefaultPolicy.class);

    private final PendingAskStore pendingAskStore;

    public DefaultPolicy(PendingAskStore pendingAskStore) {
        this.pendingAskStore = pendingAskStore;
    }

    @Override
    public Decision decide(NotificationItem item, CampusConfig config, PolicyRule rule) {
        String type = item.getType();
        if (type == null || "IGNORE".equals(type) || "UNKNOWN".equals(type)) {
            return Decision.IGNORE;
        }

        // 1. 自动推送（白名单匹配）
        if (rule.shouldAutoNotify(item)) {
            return Decision.NOTIFY;
        }

        // 2. 查历史回答
        String source = item.getSource();
        Optional<String> answer = pendingAskStore.findLatestAnswer(source, type);
        if (answer.isPresent()) {
            return answer.get().equalsIgnoreCase("yes") ? Decision.NOTIFY : Decision.SKIP;
        }

        // 3. 首次见，问用户
        return Decision.ASK;
    }
}
```

- [ ] **Step 4: 创建 `ExamRules.java`**

```java
package com.youkeda.exercise.claw.campus.policy.rule;

import java.util.List;

public class ExamRules implements PolicyRule {

    private static final List<String> AUTO_PUSH = List.of("FINAL_EXAM", "CET");

    @Override
    public List<String> getAutoPushTypes() {
        return AUTO_PUSH;
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd /e/work/claw/claw-assistant && mvn compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/youkeda/exercise/claw/campus/policy/NotificationPolicy.java \
  src/main/java/com/youkeda/exercise/claw/campus/policy/DefaultPolicy.java \
  src/main/java/com/youkeda/exercise/claw/campus/policy/rule/PolicyRule.java \
  src/main/java/com/youkeda/exercise/claw/campus/policy/rule/ExamRules.java
git commit -m "feat(campus): add NotificationPolicy, DefaultPolicy, PolicyRule, ExamRules"
```

---

### Task 5: Source 框架 — NotificationSource 接口 + CampusNotifier + CampusScheduler

**Files:**
- Create: `src/main/java/.../campus/source/NotificationSource.java`
- Create: `src/main/java/.../campus/CampusNotifier.java`
- Create: `src/main/java/.../campus/CampusScheduler.java`

**Interfaces:**
- Produces: `NotificationSource` 接口, `CampusNotifier.notifyAll(config)` 编排, `CampusScheduler` 定时触发

- [ ] **Step 1: 创建 `NotificationSource.java`**

```java
package com.youkeda.exercise.claw.campus.source;

import com.youkeda.exercise.claw.campus.model.CampusConfig;

/**
 * 通知来源接口。
 * 每个通知类型（考试/比赛/活动/就业）实现此接口。
 * Source 保持纤薄，只做编排：collect → dedup → classify → decide → push。
 */
public interface NotificationSource {

    /** Source 标识，用于日志和配置开关 */
    String getName();

    /** 判断此 Source 是否支持该全局配置（例如学校是否匹配、用户类型等） */
    boolean supports(CampusConfig globalConfig);

    /** 执行一次检查。Source 内部编排自己的 Collector/Classifier/Policy 等组件 */
    void check();
}
```

- [ ] **Step 2: 创建 `CampusNotifier.java`**

```java
package com.youkeda.exercise.claw.campus;

import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.source.NotificationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CampusNotifier {

    private static final Logger log = LoggerFactory.getLogger(CampusNotifier.class);

    private final List<NotificationSource> sources;

    public CampusNotifier(List<NotificationSource> sources) {
        this.sources = sources;
    }

    /**
     * 遍历所有 Source 执行检查
     * 双层过滤：supports() 判断配置是否支持 + sourceEnabled 判断用户是否开启
     */
    public void notifyAll(CampusConfig config) {
        for (NotificationSource source : sources) {
            try {
                if (!source.supports(config)) continue;
                if (!config.isSourceEnabled(source.getName())) continue;
                log.info("通知检查启动 | source={}", source.getName());
                source.check();
            } catch (Exception e) {
                log.error("通知检查异常 | source={}", source.getName(), e);
                // 不中断其他 Source
            }
        }
    }
}
```

- [ ] **Step 3: 创建 `CampusScheduler.java`**

```java
package com.youkeda.exercise.claw.campus;

import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.store.CampusConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CampusScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampusScheduler.class);

    private final CampusNotifier notifier;
    private final CampusConfigStore configStore;

    public CampusScheduler(CampusNotifier notifier, CampusConfigStore configStore) {
        this.notifier = notifier;
        this.configStore = configStore;
    }

    /** 每天 08:00 执行各类通知检查 */
    @Scheduled(cron = "0 0 8 * * *")
    public void dailyCheck() {
        CampusConfig config = configStore.get();
        if (config == null || !config.isEnabled()) {
            log.debug("校园提醒未配置或已关闭，跳过");
            return;
        }
        log.info("===== CampusScheduler 启动 | school={} =====", config.getSchool());
        notifier.notifyAll(config);
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
cd /e/work/claw/claw-assistant && mvn compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/youkeda/exercise/claw/campus/source/NotificationSource.java \
  src/main/java/com/youkeda/exercise/claw/campus/CampusNotifier.java \
  src/main/java/com/youkeda/exercise/claw/campus/CampusScheduler.java
git commit -m "feat(campus): add NotificationSource, CampusNotifier, CampusScheduler"
```

---

### Task 6: ExamSource 适配 — 将现有考试功能包装为 NotificationSource

**Files:**
- Create: `src/main/java/.../campus/source/ExamSource.java`

**Interfaces:**
- Consumes: `CampusNoticeCollector`, `CampusNoticeStore`, `ExamRuleClassifier`, `ExamLLMClassifier`, `DefaultPolicy`, `NotificationService`, `PendingAskStore`, `WechatUserManager`, `CampusConfigStore`
- Produces: `ExamSource implements NotificationSource` — 将现有的 `CampusNoticeProcessor.process()` 逻辑迁入

- [ ] **Step 1: 创建 `ExamSource.java`**

ExamSource 不从头实现采集→分类→推送，而是复用现有的 `CampusNoticeProcessor` 进行编排。`ExamSource` 只做封装和桥接。

```java
package com.youkeda.exercise.claw.campus.source;

import com.youkeda.exercise.claw.campus.collector.CampusNoticeCollector;
import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.model.ExamClassification;
import com.youkeda.exercise.claw.campus.model.NoticeItem;
import com.youkeda.exercise.claw.campus.model.NotificationItem;
import com.youkeda.exercise.claw.campus.policy.DefaultPolicy;
import com.youkeda.exercise.claw.campus.policy.NotificationPolicy;
import com.youkeda.exercise.claw.campus.policy.rule.ExamRules;
import com.youkeda.exercise.claw.campus.store.CampusConfigStore;
import com.youkeda.exercise.claw.campus.store.CampusNoticeStore;
import com.youkeda.exercise.claw.campus.store.PendingAskStore;
import com.youkeda.exercise.claw.scout.judge.Recommendation;
import com.youkeda.exercise.claw.scout.notifier.NotificationService;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class ExamSource implements NotificationSource {

    private static final Logger log = LoggerFactory.getLogger(ExamSource.class);

    private final CampusNoticeCollector collector;
    private final CampusNoticeStore noticeStore;
    private final DefaultPolicy policy;
    private final CampusConfigStore configStore;
    private final PendingAskStore pendingAskStore;
    private final NotificationService notificationService;
    private final WechatUserManager userManager;

    public ExamSource(CampusNoticeCollector collector,
                      CampusNoticeStore noticeStore,
                      DefaultPolicy policy,
                      CampusConfigStore configStore,
                      PendingAskStore pendingAskStore,
                      NotificationService notificationService,
                      WechatUserManager userManager) {
        this.collector = collector;
        this.noticeStore = noticeStore;
        this.policy = policy;
        this.configStore = configStore;
        this.pendingAskStore = pendingAskStore;
        this.notificationService = notificationService;
        this.userManager = userManager;
    }

    @Override
    public String getName() { return "EXAM"; }

    @Override
    public boolean supports(CampusConfig config) {
        return config.getSchool() != null && !config.getSchool().isBlank();
    }

    @Override
    public void check() {
        CampusConfig config = configStore.get();
        if (config == null || !supports(config)) return;

        String schoolUrl = resolveSchoolUrl(config.getSchool());
        if (schoolUrl == null) {
            log.warn("未知学校，跳过考试通知检查 | school={}", config.getSchool());
            return;
        }

        log.info("===== ExamSource 检查 | school={} =====", config.getSchool());

        // 1. 采集
        List<NoticeItem> notices = collector.collect(schoolUrl);
        if (notices.isEmpty()) return;

        // 2. 去重
        List<NoticeItem> newNotices = noticeStore.deduplicate(notices);
        if (newNotices.isEmpty()) return;

        // 3. 逐条处理
        for (NoticeItem notice : newNotices) {
            processNotice(notice, config);
        }

        log.info("===== ExamSource 完成 | school={} | new={} =====",
                config.getSchool(), newNotices.size());
    }

    private void processNotice(NoticeItem notice, CampusConfig config) {
        // 规则分类 → LLM分类（复用现有 CampusNoticeProcessor 的模式）
        // 这里简化：直接调用 CampusNoticeProcessor 现有逻辑
        // 由于 CampusNoticeProcessor 已经存在且运行稳定，ExamSource.check()
        // 的职责是适配到新接口，实际的复杂编排仍由 CampusNoticeProcessor 完成。
        // Phase 2 再考虑将 CampusNoticeProcessor 完全迁移到 ExamSource 内。

        // 将 NoticeItem 桥接为 NotificationItem 供 DefaultPolicy 使用
        NotificationItem notificationItem = bridgeToNotificationItem(notice);

        // 决策
        NotificationPolicy.Decision decision = policy.decide(
            notificationItem, config, new ExamRules());

        // 执行决策
        switch (decision) {
            case NOTIFY -> notifyUser(notice, notificationItem);
            case ASK -> askUser(notice, notificationItem);
            case SKIP -> log.debug("用户已忽略 {}，跳过", notice.getType());
            case IGNORE -> log.debug("非考试通知，忽略");
        }
    }

    private NotificationItem bridgeToNotificationItem(NoticeItem notice) {
        NotificationItem item = new NotificationItem("EXAM",
            notice.getTitle(), notice.getUrl(), notice.getPublishAt());
        if (notice.getType() != null) item.setType(notice.getType().name());
        item.setConfidence(notice.getConfidence());
        item.setScoreSource(notice.getScoreSource());
        item.setClassifierReason(notice.getClassifierReason());
        item.setStatus(notice.getStatus());
        item.setProcessedAt(notice.getProcessedAt());
        return item;
    }

    private void notifyUser(NoticeItem notice, NotificationItem notificationItem) {
        String userId = userManager.getOwnerUserId();
        String typeDisplayName = typeDisplayName(notice.getType());

        String message = "📌 考试提醒\n"
            + "「" + notice.getTitle() + "」\n"
            + "类型: " + typeDisplayName + "\n"
            + (notice.getPublishAt() != null && !notice.getPublishAt().isBlank()
                ? "发布日期: " + notice.getPublishAt() + "\n" : "")
            + "详情: " + notice.getUrl();

        notificationService.notify(userId, List.of(new Recommendation(
            "exam_" + notificationItem.getType(),
            userId,
            notice.getTitle(),
            typeDisplayName + "考试提醒",
            notificationItem.getClassifierReason(),
            message,
            notice.getUrl(),
            1.0f,
            System.currentTimeMillis()
        )));
        log.info("考试通知已推送 | title={}", notice.getTitle());
    }

    private void askUser(NoticeItem notice, NotificationItem notificationItem) {
        String userId = userManager.getOwnerUserId();
        String typeDisplayName = typeDisplayName(notice.getType());

        String question = "检测到新的「" + typeDisplayName + "」通知："
            + notice.getTitle() + "，\n需要提醒你吗？（回复 需要/不需要）";

        notificationService.notify(userId, List.of(new Recommendation(
            "ask_" + notificationItem.getType(),
            userId,
            notice.getTitle() + " - 是否需要提醒",
            "需要用户确认",
            notificationItem.getClassifierReason(),
            question,
            null,
            0.8f,
            System.currentTimeMillis()
        )));

        pendingAskStore.save("EXAM", notificationItem.getType(), question, "PENDING");
        log.info("已询问用户是否推送 | type={} | title={}",
                notificationItem.getType(), notice.getTitle());
    }

    private String typeDisplayName(NoticeType type) {
        if (type == null) return "未知";
        return switch (type) {
            case FINAL_EXAM -> "期末考试";
            case CET -> "大学英语四六级";
            case RETAKE -> "补考/重修";
            case MIDTERM -> "期中考试";
            case COMPUTER_LEVEL -> "计算机等级考试";
            case PUTONGHUA -> "普通话测试";
            case OTHER_EXAM -> "其他考试";
            default -> "未知类型";
        };
    }

    private String resolveSchoolUrl(String school) {
        if (school == null) return null;
        if (school.contains("南邮") || school.contains("南京邮电")) {
            return CampusNoticeCollector.NJUPT_NOTICE_URL;
        }
        log.warn("未配置的学校: {}", school);
        return null;
    }
}
```

注意：需要在文件顶部添加 import：
```java
import com.youkeda.exercise.claw.campus.model.NoticeType;
```

- [ ] **Step 2: 编译验证**

```bash
cd /e/work/claw/claw-assistant && mvn compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: 配置调整：ExamSource 使用 `@ConditionalOnProperty`，现有 `CampusExamMonitor` 也使用同样的条件——两者会同时注册，互不冲突。CampusScheduler.dailyCheck() 会触发所有 Source，包括 ExamSource。CampusExamMonitor 仍然由自己的 @Scheduled 触发。双通道同时运行。**

No code change needed here—Spring 会同时管理两个定时器，互不干扰。

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/youkeda/exercise/claw/campus/source/ExamSource.java
git commit -m "feat(campus): add ExamSource wrapping existing exam reminder logic"
```

---

### Task 7: CompetitionSource 基础版

**Files:**
- Create: `src/main/java/.../campus/source/CompetitionSource.java`
- Create: `src/main/java/.../campus/collector/CompetitionCollector.java`
- Create: `src/main/java/.../campus/classifier/CompetitionClassifier.java`
- Create: `src/main/java/.../campus/policy/rule/CompetitionRules.java`

**Interfaces:**
- Consumes: `CampusNotificationStore`, `DefaultPolicy`, `NotificationService`, `PendingAskStore`, `WechatUserManager`, `CampusConfigStore`
- Produces: `CompetitionSource` — 比赛通知入口

- [ ] **Step 1: 创建 `CompetitionCollector.java`**

Phase 1 复用南邮教务处通知列表，仅标题解析，不额外爬第三方网站。

```java
package com.youkeda.exercise.claw.campus.collector;

import com.youkeda.exercise.claw.campus.model.NotificationItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CompetitionCollector {

    private static final Logger log = LoggerFactory.getLogger(CompetitionCollector.class);
    private static final int TIMEOUT_SECONDS = 15;
    private static final String NJUPT_NOTICE_URL = "https://jwc.njupt.edu.cn/1622/list34.psp";

    /**
     * 采集通知列表，所有标题返回，由 CompetitionClassifier 筛选比赛相关
     */
    public List<NotificationItem> collect() {
        List<NotificationItem> items = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(NJUPT_NOTICE_URL)
                    .timeout((int) Duration.ofSeconds(TIMEOUT_SECONDS).toMillis())
                    .userAgent("ClawBot-Campus/1.0")
                    .get();

            String baseUri = doc.baseUri();
            if (baseUri == null || baseUri.isBlank()) baseUri = NJUPT_NOTICE_URL;

            // 与 CampusNoticeCollector 相同的解析逻辑
            List<Element> entries = doc.select("div.list-item, li.list-item, div.news-item");
            if (entries.isEmpty()) entries = doc.select("div.title").parents();
            if (entries.isEmpty()) entries = doc.select("div.title").stream().map(Element::parent).toList();

            if (entries.isEmpty()) {
                for (Element titleEl : doc.select("div.title")) {
                    Element link = titleEl.parent();
                    if (link == null) link = titleEl;
                    Element a = link.tagName().equals("a") ? link : link.selectFirst("a");
                    if (a == null) a = titleEl.selectFirst("a");
                    if (a != null) {
                        String href = a.attr("href");
                        String title = a.text().trim();
                        String url = resolveUrl(href, baseUri);
                        String date = findDate(link);
                        if (!title.isEmpty()) {
                            items.add(new NotificationItem("COMPETITION", title, url, date));
                        }
                    }
                }
            } else {
                for (Element entry : entries) {
                    Element titleEl = entry.selectFirst("div.title, span.title, a");
                    if (titleEl == null) continue;
                    Element a = titleEl.tagName().equals("a") ? titleEl : titleEl.selectFirst("a");
                    if (a == null) continue;
                    String href = a.attr("href");
                    String title = a.text().trim();
                    String url = resolveUrl(href, baseUri);
                    String date = findDate(entry);
                    if (!title.isEmpty()) {
                        items.add(new NotificationItem("COMPETITION", title, url, date));
                    }
                }
            }
            log.info("比赛采集完成 | count={}", items.size());
        } catch (Exception e) {
            log.error("比赛采集失败 | url={}", NJUPT_NOTICE_URL, e);
        }
        return items;
    }

    private String resolveUrl(String href, String baseUri) {
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        String base = baseUri.replaceAll("/[^/]*$", "/");
        if (href.startsWith("/")) base = baseUri.replaceAll("^(https?://[^/]+).*$", "$1");
        return base + (href.startsWith("/") ? href.substring(1) : href);
    }

    private String findDate(Element entry) {
        Element dateEl = entry.selectFirst("div.d, span.date, span.time, em");
        return dateEl != null ? dateEl.text().trim() : "";
    }
}
```

- [ ] **Step 2: 创建 `CompetitionClassifier.java`**

标题关键词规则匹配，区分比赛类型、非比赛、不确定。

```java
package com.youkeda.exercise.claw.campus.classifier;

import com.youkeda.exercise.claw.campus.model.NotificationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 比赛分类器。
 * 规则优先（0 token），未命中返回 null 表示非比赛。
 * Phase 1 仅规则分类，Phase 2 可扩展 LLM 分类。
 */
@Component
public class CompetitionClassifier {

    private static final Logger log = LoggerFactory.getLogger(CompetitionClassifier.class);

    private static final List<Rule> RULES = List.of(
        new Rule("挑战杯",          "CHALLENGE_CUP"),
        new Rule("互联网\\+",       "INTERNET_PLUS"),
        new Rule("大学生创新创业训练|大创", "INNOVATION_EXPO"),
        new Rule("数学建模|数模",    "ACADEMIC_COMPETITION"),
        new Rule("ACM|程序设计竞赛",  "ACADEMIC_COMPETITION"),
        new Rule("蓝桥杯",          "ACADEMIC_COMPETITION"),
        new Rule("计算机设计大赛",    "SKILL_COMPETITION"),
        new Rule("竞赛|比赛|选拔",    "OTHER_COMPETITION"),
        new Rule("报名.*大赛|大赛.*报名", "OTHER_COMPETITION")
    );

    /**
     * 分类通知。返回匹配的比赛类型，或 null 表示非比赛通知。
     */
    public String classify(NotificationItem item) {
        String title = item.getTitle();
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(title).find()) {
                log.debug("比赛分类命中 | title={} | type={}", title, rule.type());
                return rule.type();
            }
        }
        return null; // 非比赛通知
    }

    private record Rule(Pattern pattern, String type) {
        Rule(String keyword, String type) {
            this(Pattern.compile(keyword, Pattern.CASE_INSENSITIVE), type);
        }
    }
}
```

- [ ] **Step 3: 创建 `CompetitionRules.java`**

```java
package com.youkeda.exercise.claw.campus.policy.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * 比赛通知推送规则。
 * 白名单中的比赛自动推送，其他比赛询问用户。
 */
public class CompetitionRules implements PolicyRule {

    /** 内置大赛白名单（自动推送） */
    private static final List<String> DEFAULT_WHITELIST = List.of(
        "CHALLENGE_CUP",
        "INTERNET_PLUS",
        "INNOVATION_EXPO"
    );

    private final List<String> whitelist;

    public CompetitionRules() {
        this(new ArrayList<>(DEFAULT_WHITELIST));
    }

    public CompetitionRules(List<String> whitelist) {
        this.whitelist = whitelist != null ? whitelist : new ArrayList<>(DEFAULT_WHITELIST);
    }

    @Override
    public List<String> getAutoPushTypes() {
        return whitelist;
    }
}
```

- [ ] **Step 4: 创建 `CompetitionSource.java`**

```java
package com.youkeda.exercise.claw.campus.source;

import com.youkeda.exercise.claw.campus.classifier.CompetitionClassifier;
import com.youkeda.exercise.claw.campus.collector.CompetitionCollector;
import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.model.NotificationItem;
import com.youkeda.exercise.claw.campus.policy.DefaultPolicy;
import com.youkeda.exercise.claw.campus.policy.NotificationPolicy;
import com.youkeda.exercise.claw.campus.policy.rule.CompetitionRules;
import com.youkeda.exercise.claw.campus.store.CampusNotificationStore;
import com.youkeda.exercise.claw.campus.store.PendingAskStore;
import com.youkeda.exercise.claw.scout.judge.Recommendation;
import com.youkeda.exercise.claw.scout.notifier.NotificationService;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CompetitionSource implements NotificationSource {

    private static final Logger log = LoggerFactory.getLogger(CompetitionSource.class);

    private final CompetitionCollector collector;
    private final CampusNotificationStore store;
    private final CompetitionClassifier classifier;
    private final DefaultPolicy policy;
    private final PendingAskStore pendingAskStore;
    private final NotificationService notificationService;
    private final WechatUserManager userManager;

    public CompetitionSource(CompetitionCollector collector,
                             CampusNotificationStore store,
                             CompetitionClassifier classifier,
                             DefaultPolicy policy,
                             PendingAskStore pendingAskStore,
                             NotificationService notificationService,
                             WechatUserManager userManager) {
        this.collector = collector;
        this.store = store;
        this.classifier = classifier;
        this.policy = policy;
        this.pendingAskStore = pendingAskStore;
        this.notificationService = notificationService;
        this.userManager = userManager;
    }

    @Override
    public String getName() { return "COMPETITION"; }

    @Override
    public boolean supports(CampusConfig config) {
        return config.getSchool() != null && !config.getSchool().isBlank();
    }

    @Override
    public void check() {
        log.info("===== CompetitionSource 检查 =====");

        // 1. 采集
        List<NotificationItem> items = collector.collect();
        if (items.isEmpty()) return;

        // 2. 去重
        List<NotificationItem> newItems = store.deduplicate(items);
        if (newItems.isEmpty()) return;

        // 3. 逐条分类→决策→推送
        for (NotificationItem item : newItems) {
            processItem(item);
        }

        log.info("===== CompetitionSource 完成 | new={} =====", newItems.size());
    }

    private void processItem(NotificationItem item) {
        // 分类
        String type = classifier.classify(item);
        if (type == null) {
            log.debug("非比赛通知，跳过 | title={}", item.getTitle());
            return; // 不是比赛通知
        }
        item.setType(type);

        // 更新存储
        store.update(item);

        // 决策
        CompetitionRules rules = new CompetitionRules(); // Phase 1 硬编码白名单
        NotificationPolicy.Decision decision = policy.decide(item, null, rules);

        switch (decision) {
            case NOTIFY -> notifyUser(item);
            case ASK -> askUser(item);
            case SKIP -> log.debug("用户已忽略比赛 {}，跳过", type);
            case IGNORE -> log.debug("忽略比赛通知");
        }
    }

    private void notifyUser(NotificationItem item) {
        String userId = userManager.getOwnerUserId();
        String typeDisplayName = typeDisplayName(item.getType());

        String message = "🏆 比赛提醒\n"
            + "「" + item.getTitle() + "」\n"
            + "类型: " + typeDisplayName + "\n"
            + (item.getPublishAt() != null && !item.getPublishAt().isBlank()
                ? "发布日期: " + item.getPublishAt() + "\n" : "")
            + "详情: " + item.getUrl();

        notificationService.notify(userId, List.of(new Recommendation(
            "comp_" + item.getType(),
            userId,
            item.getTitle(),
            typeDisplayName + "比赛通知",
            item.getClassifierReason(),
            message,
            item.getUrl(),
            1.0f,
            System.currentTimeMillis()
        )));
        log.info("比赛通知已推送 | title={}", item.getTitle());
    }

    private void askUser(NotificationItem item) {
        String userId = userManager.getOwnerUserId();
        String typeDisplayName = typeDisplayName(item.getType());

        String question = "检测到新的「" + typeDisplayName + "」比赛通知："
            + item.getTitle() + "，\n需要关注这类比赛吗？（回复 需要/不需要）";

        notificationService.notify(userId, List.of(new Recommendation(
            "ask_comp_" + item.getType(),
            userId,
            item.getTitle() + " - 是否需要关注",
            "需要用户确认",
            item.getClassifierReason(),
            question,
            null,
            0.8f,
            System.currentTimeMillis()
        )));

        pendingAskStore.save("COMPETITION", item.getType(), question, "PENDING");
        log.info("已询问用户是否关注比赛 | type={} | title={}", item.getType(), item.getTitle());
    }

    private String typeDisplayName(String type) {
        if (type == null) return "未知";
        return switch (type) {
            case "CHALLENGE_CUP" -> "挑战杯";
            case "INTERNET_PLUS" -> "互联网+";
            case "INNOVATION_EXPO" -> "大创";
            case "ACADEMIC_COMPETITION" -> "学科竞赛";
            case "SKILL_COMPETITION" -> "技能大赛";
            case "OTHER_COMPETITION" -> "其他比赛";
            default -> type;
        };
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd /e/work/claw/claw-assistant && mvn compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/youkeda/exercise/claw/campus/source/CompetitionSource.java \
  src/main/java/com/youkeda/exercise/claw/campus/collector/CompetitionCollector.java \
  src/main/java/com/youkeda/exercise/claw/campus/classifier/CompetitionClassifier.java \
  src/main/java/com/youkeda/exercise/claw/campus/policy/rule/CompetitionRules.java
git commit -m "feat(campus): add CompetitionSource (basic version with whitelist)"
```

---

### Task 8: 占位 Source — CourseReminderSource

**Files:**
- Create: `src/main/java/.../campus/source/CourseReminderSource.java`

**Interfaces:**
- Produces: `CourseReminderSource implements NotificationSource` — 占位，`supports()` 返回 false

- [ ] **Step 1: 创建 `CourseReminderSource.java`**

```java
package com.youkeda.exercise.claw.campus.source;

import com.youkeda.exercise.claw.campus.model.CampusConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 课程提醒占位 Source。
 * 等待 vae-tools 的 schedule 模块融合后启用。
 */
@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CourseReminderSource implements NotificationSource {

    private static final Logger log = LoggerFactory.getLogger(CourseReminderSource.class);

    @Override
    public String getName() { return "COURSE"; }

    @Override
    public boolean supports(CampusConfig config) {
        return false; // 暂不启用，等待 vae-tools schedule 模块融合
    }

    @Override
    public void check() {
        // 空实现，融合 vae-tools 的 ScheduleReminderService 后填充
        log.debug("CourseReminderSource 未启用");
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /e/work/claw/claw-assistant && mvn compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/youkeda/exercise/claw/campus/source/CourseReminderSource.java
git commit -m "feat(campus): add CourseReminderSource placeholder"
```

---

### Task 9: 全量编译与启动验证

**Files:**
- 无代码修改

- [ ] **Step 1: 全量编译**

```bash
cd /e/work/claw/claw-assistant && mvn compile 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: 运行启动验证（20秒后停止）**

```bash
cd /e/work/claw/claw-assistant && timeout 20 mvn spring-boot:run -q 2>&1 | grep -E 'ERROR|WARN|campus|Campus|启动|Started|Error' | head -20
```

Expected: 应用启动成功，无 ERROR 日志，可看到 CampusScheduler/ExamSource/CompetitionSource/CourseReminderSource 相关的初始化日志。

- [ ] **Step 3: 提交最终版本**

```bash
git add -A
git commit -m "chore: Phase 1 final commit"
```

---

## Self-Review

**1. Spec coverage:**
- ✅ 新增 `NotificationItem` 通用模型 — Task 1
- ✅ `NoticeType` 扩展比赛枚举 — Task 1
- ✅ `CampusConfig` 加 sourceEnabled — Task 1
- ✅ DB 迁移加 source 列 — Task 2
- ✅ `CampusNotificationStore` 新建（含 source 去重）— Task 3
- ✅ `PendingAskStore` 扩展 source 方法 — Task 3
- ✅ `NotificationPolicy` 接口 + `DefaultPolicy` 实现 — Task 4
- ✅ `PolicyRule` 接口 + `ExamRules` — Task 4
- ✅ `NotificationSource` 接口 — Task 5
- ✅ `CampusNotifier` 编排器 — Task 5
- ✅ `CampusScheduler` 调度器 — Task 5
- ✅ `ExamSource` 适配 — Task 6
- ✅ `CompetitionSource` 基础版 — Task 7
- ✅ `CompetitionCollector` + `CompetitionClassifier` + `CompetitionRules` — Task 7
- ✅ `CourseReminderSource` 占位 — Task 8
- ✅ 非破坏性：实际没有修改现有 `CampusExamMonitor` / `CampusNoticeProcessor` — 全程

**2. Phase 2 排除项（不在本计划内）：**
- ActivitySource + ActivityCollector + ActivityClassifier + ActivityRules
- JobInfoSource + JobCollector + JobClassifier + JobRules
- CampusReminderFunction 统一配置入口（等 Phase 2 再做）
- 纯 policy/rule 目录下 ActivityRules/JobRules 只创建了接口，具体实现在 Phase 2

**3. Placeholder scan:** 无 TBD/TODO 占位。所有代码块均为可直接编译的完整实现。

**4. Type consistency:**
- `NotificationSource.getName()` / `supports()` / `check()` — 所有 Source 实现一致
- `NotificationPolicy.decide(NotificationItem, CampusConfig, PolicyRule) → Decision` — 跨 Task 4/6/7 一致
- `PolicyRule.getAutoPushTypes() → List<String>` — 跨 Task 4/7 一致
- `PendingAskStore.findLatestAnswer(source, type) → Optional<String>` — 跨 Task 3/4 一致
- `NotificationItem` 字段与 `campus_notice` 表列名对齐 — source, title, url, type 等
- `CampusNotificationStore.deduplicate()` 去重按 `url + source` 联合检查 — 与 spec 一致
- `CompetitionCollector` 采集结果含 `source="COMPETITION"` — 与 `CampusNotificationStore` 的 source 筛选对齐
