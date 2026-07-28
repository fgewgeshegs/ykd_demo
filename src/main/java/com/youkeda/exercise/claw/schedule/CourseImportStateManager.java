package com.youkeda.exercise.claw.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 课程导入状态管理器
 *
 * <p>管理用户导入课表的多步骤流程状态。
 * 流程：WAITING_FILE → (收到文件/图片) → WAITING_CONFIRM → (用户确认) → 完成
 *
 * <p>使用内存状态，重启后丢失（用户重试即可）。
 * 待确认的 {@link CourseEntity} 列表暂存于内存，确认后写入 SQLite {@code course_schedule} 表。
 */
@Component
public class CourseImportStateManager {

    private static final Logger log = LoggerFactory.getLogger(CourseImportStateManager.class);

    /** 导入阶段 */
    public enum Phase {
        /** 无进行中的导入 */
        NONE,
        /** 等待用户上传课表文件/图片 */
        WAITING_FILE,
        /** 课表已解析，等待用户确认 */
        WAITING_CONFIRM
    }

    /** 用户导入状态 */
    public record ImportState(
            Phase phase,
            String fileAnalysis
    ) {
        public static ImportState none() {
            return new ImportState(Phase.NONE, null);
        }
    }

    private final ConcurrentMap<String, ImportState> userStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<CourseEntity>> pendingCourses = new ConcurrentHashMap<>();

    // ==================== 状态管理 ====================

    public void setWaitingFile(String userId) {
        userStates.put(userId, new ImportState(Phase.WAITING_FILE, null));
        pendingCourses.remove(userId);
        log.debug("导入状态：等待文件 | userId={}", userId);
    }

    public void setWaitingConfirm(String userId, String fileAnalysis) {
        userStates.put(userId, new ImportState(Phase.WAITING_CONFIRM, fileAnalysis));
        log.debug("导入状态：等待确认 | userId={}", userId);
    }

    public void clear(String userId) {
        userStates.remove(userId);
        pendingCourses.remove(userId);
        log.debug("导入状态已清除 | userId={}", userId);
    }

    public Phase getPhase(String userId) {
        return userStates.getOrDefault(userId, ImportState.none()).phase();
    }

    public ImportState getState(String userId) {
        return userStates.getOrDefault(userId, ImportState.none());
    }

    // ==================== 待确认课程 ====================

    public void setPendingCourses(String userId, List<CourseEntity> courses) {
        pendingCourses.put(userId, List.copyOf(courses));
        log.debug("待确认课程已保存 | userId={} | count={}", userId, courses.size());
    }

    public List<CourseEntity> getPendingCourses(String userId) {
        return pendingCourses.getOrDefault(userId, List.of());
    }

    // ==================== 统计 ====================

    public int waitingFileCount() {
        return (int) userStates.values().stream()
                .filter(s -> s.phase() == Phase.WAITING_FILE)
                .count();
    }

    public int waitingConfirmCount() {
        return (int) userStates.values().stream()
                .filter(s -> s.phase() == Phase.WAITING_CONFIRM)
                .count();
    }
}