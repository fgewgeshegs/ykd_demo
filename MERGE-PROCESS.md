# 分支合并操作流程

> 面向组员和 Claude 的标准化合并流程，**严格按顺序执行**。

---

## 一、建立基线

```bash
# 1. 切到目标分支
git checkout <your-branch>        # 你想合入的分支

# 2. 确保工作区干净
git status --short                # 应为空

# 3. 获取远端分支
git fetch origin <source-branch>  # 来源分支
```

---

## 二、全面比对（不要直接 merge）

先看全貌，再决定合并方案。

### 2.1 整体概览

```bash
# 新增/修改/删除了哪些文件
git diff --stat <your-branch>..origin/<source-branch>

# commit 历史
git log --oneline <your-branch>..origin/<source-branch>
```

### 2.2 逐文件审查（重点关注）

对以下类型的文件做逐行 diff：

```bash
# 所有 Java 文件
git diff <your-branch>..origin/<source-branch> -- src/main/java/

# 资源配置
git diff <your-branch>..origin/<source-branch> -- src/main/resources/

# 构建配置
git diff <your-branch>..origin/<source-branch> -- pom.xml

# git 忽略规则
git diff <your-branch>..origin/<source-branch> -- .gitignore
```

### 2.3 标注三类差异

| 类型 | 标注 | 举例 |
|------|------|------|
| **新增** | 🟢 完全新增的文件或配置 | 新模块、新工具类 |
| **修改** | 🟡 双方都改过的文件 | 同一方法的逻辑调整 |
| **删除** | 🔴 被删除的文件 | 废弃功能的清理 |

---

## 三、方案确认（最难的一步）

对每个 **修改** 和 **删除** 的文件，回答三个问题：

### 3.1 冲突型差异

> 两边改了同一块代码

- 选哪边？还是两边合？
- 决策依据：**功能完整性 > 代码简洁性 > 行数**

### 3.2 删除型差异

> 对方删了这个文件，我们要不要跟着删？

确认清单：

- [ ] 文件在生产代码中还有引用吗？（`grep -r` 搜一下）
- [ ] 是 `@Deprecated` 标记的废弃代码吗？
- [ ] 删除后编译能过吗？
- [ ] 如果有单元测试引用，测试依赖删了吗？

### 3.3 配置型差异

> `application.properties` / `.gitignore` / `pom.xml`

- 重复的配置段去重
- 版本号冲突（依赖版本、插件版本）
- `.gitignore` 的增删是否会影响已跟踪文件

### 3.4 确认清单

每个决策点需要明确告诉 Claude：

```
【保留/选用哪边】xxx — 理由：xxxx
```

---

## 四、执行合并

### 4.1 执行 merge

```bash
git merge origin/<source-branch>
```

### 4.2 解决冲突（4 种情况）

#### 情况 A：对方新增 / 我方未改 → 自动合并 ✅

无需处理。

#### 情况 B：两边都改 → 有冲突（UU 状态）

```bash
# 查看全部冲突文件
git status --short | grep '^UU'
```

对每个冲突文件：

1. **先看 diff 理解两边各想干嘛**：哪边的改动是"功能增强"？哪边是"修复"？
2. **合并策略优先级**：
   - 🥇 两个都保留（手动拼接）
   - 🥈 选功能更完整的那边
   - 🥉 两边各取一部分
3. **编辑文件解决冲突段**：
   ```
   <<<<<<< HEAD
   （我方的代码）
   =======
   （对方的代码）
   >>>>>>> origin/<branch>
   ```
   → 删掉标记线，留下你要的代码
4. **标记已解决**：
   ```bash
   git add <file>
   ```

#### 情况 C：对方删除 / 我方没改 → 自动删除 ✅

`git status` 中显示为 `D`，无需处理。

#### 情况 D：对方删除 / 我方改了 → 冲突

- 我方的改动有价值吗？→ 保留我方改动，不等同删除
- 我方改动是无意义的残留？→ 接受删除

### 4.3 解决非冲突差异（M 状态）

有些文件自动合并成功了，但**两边逻辑加在一起可能有重复或矛盾**。

例如 `LLMClient.java`：

```java
// 自动合并可能变成这样（双写 reasoning_content）：
node.put("reasoning_content", msg.reasoningContent());  // 原来是外层的
node.put("reasoning_content", msg.reasoningContent());  // 原来是内层 tool_call 的
```

→ 手动审阅自动合并结果，去重。

### 4.4 执行额外约定

合并完成后，做提交前约定的清理：

- 删除僵尸接口/类
- 去重配置
- 调整包路径
- 更新注释

---

## 五、编译验证（必须通过）

```bash
mvn clean compile
# 如果有测试
mvn test
```

**编译失败时禁止提交。**

---

## 六、提交

```bash
git add -A
```

提交信息模板：

```
chore: merge <source-branch> — <主要功能概括>

新功能：
- xxx：xxx 模块（xxx技术栈）
- xxx：xxx 配置新增

保留（本地分支原有功能不受影响）：
- xxx 机制保持不变
- xxx 参数值保持不变

修复（合并冲突解决）：
- xxx: 这里改了啥逻辑，为什么
- xxx: 去重/合并了什么

清理：
- 删除 xxx（僵尸代码/冗余配置）
- 去重 xxx 配置段
```

---

## 七、两次检查清单

提交后验证：

- [ ] `git log --oneline -3` 确认 commit 在正确分支上
- [ ] `mvn clean compile` 通过（最终确认）
- [ ] `git diff HEAD~1 --stat` 确认修改范围合理（没有误改不该动的文件）
- [ ] 需要协调通知其他人吗？（比如删了他们引用的 API）

---

## CLI 速查卡

```bash
# 查看完整 diff
git diff <branch>..origin/<branch> -- <path>

# 看合并基础点
git merge-base HEAD origin/<branch>

# 看"从合并基础点到当前分支"的改动
git diff <merge-base>..HEAD -- <file>

# 看"从合并基础点到对方分支"的改动
git diff <merge-base>..origin/<branch> -- <file>
```

---

> **关键原则**：先分析，再决策，最后执行。不要在合并过程中临时做决定。
