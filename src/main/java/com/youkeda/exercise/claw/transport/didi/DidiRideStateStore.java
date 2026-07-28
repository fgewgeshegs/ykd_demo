package com.youkeda.exercise.claw.transport.didi;

import com.youkeda.exercise.claw.transport.didi.model.TaxiEstimateRequest;
import com.youkeda.exercise.claw.transport.didi.model.TaxiEstimateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 滴滴打车多轮状态存储
 *
 * <p>管理每个用户的打车流程状态，确保 {@code estimate → confirm → create_order} 的有序流转。
 * 使用 {@link ConcurrentHashMap} 保证多用户并发安全。
 *
 * <p>状态机：
 * <pre>
 *   ESTIMATED        — 估价完成，等待用户确认
 *       ↓
 *   WAITING_CONFIRM  — 用户已确认（LLM 调用 confirm），等待创建订单
 *       ↓
 *   ORDER_CREATED    — 订单创建成功
 * </pre>
 *
 * <p>不依赖 LLM 上下文，即使 LLM 上下文丢失（重试/超时），
 * 也能从状态存储中恢复打车进度。
 */
@Component
public class DidiRideStateStore {

    private static final Logger log = LoggerFactory.getLogger(DidiRideStateStore.class);

    private final ConcurrentMap<String, RideState> store = new ConcurrentHashMap<>();

    // ==================== 状态枚举 ====================

    /**
     * 打车流程状态
     */
    public enum RideStatus {
        /** 估价完成，等待用户确认 */
        ESTIMATED,
        /** 用户已确认，等待创建订单 */
        WAITING_CONFIRM,
        /** 订单已创建 */
        ORDER_CREATED
    }

    // ==================== 写操作 ====================

    /**
     * 保存估价结果，设置状态为 {@link RideStatus#ESTIMATED}。
     * 如果已存在 ORDER_CREATED 状态的记录，则拒绝覆盖。
     *
     * @param userId  用户标识
     * @param request 估价请求参数
     * @param response 估价响应结果
     * @throws IllegalStateException 如果已有进行中的订单
     */
    public void saveEstimate(String userId, TaxiEstimateRequest request, TaxiEstimateResponse response) {
        RideState existing = store.get(userId);
        if (existing != null && existing.status == RideStatus.ORDER_CREATED) {
            throw new IllegalStateException("用户 " + userId + " 已有进行中的订单，无法重新估价");
        }

        RideState state = new RideState(
                userId,
                request,
                response,
                response != null ? response.getTraceId() : null,
                null, // orderId
                RideStatus.ESTIMATED,
                System.currentTimeMillis()
        );
        store.put(userId, state);
        log.info("状态更新 | userId={} | status=ESTIMATED | traceId={}",
                userId, response != null ? response.getTraceId() : "null");
    }

    /**
     * 确认订单（用户确认价格后调用），将状态从 {@link RideStatus#ESTIMATED} 转为
     * {@link RideStatus#WAITING_CONFIRM}。
     *
     * @param userId 用户标识
     * @throws IllegalStateException 如果状态不是 ESTIMATED
     */
    public void confirmBooking(String userId) {
        RideState state = store.get(userId);
        if (state == null) {
            throw new IllegalStateException("未找到打车估价记录，请先调用 estimate");
        }
        if (state.status == RideStatus.ORDER_CREATED) {
            throw new IllegalStateException("订单已创建，无需重复确认");
        }
        if (state.status != RideStatus.ESTIMATED) {
            throw new IllegalStateException("当前状态不允许确认：期望 ESTIMATED，实际 " + state.status);
        }

        RideState updated = new RideState(
                state.userId,
                state.estimateRequest,
                state.estimateResponse,
                state.traceId,
                state.orderId,
                RideStatus.WAITING_CONFIRM,
                System.currentTimeMillis()
        );
        store.put(userId, updated);
        log.info("状态更新 | userId={} | status=WAITING_CONFIRM | traceId={}",
                userId, state.traceId);
    }

    /**
     * 保存订单 ID，更新状态为 {@link RideStatus#ORDER_CREATED}。
     *
     * @param userId  用户标识
     * @param orderId 订单 ID
     * @throws IllegalStateException 如果状态不是 WAITING_CONFIRM
     */
    public void saveOrder(String userId, String orderId) {
        RideState state = store.get(userId);
        if (state == null) {
            throw new IllegalStateException("未找到打车估价记录，请先调用 estimate");
        }
        if (state.status != RideStatus.WAITING_CONFIRM) {
            throw new IllegalStateException("订单未获得用户确认：期望 WAITING_CONFIRM，实际 " + state.status
                    + "。请先等待用户确认再创建订单");
        }

        RideState updated = new RideState(
                state.userId,
                state.estimateRequest,
                state.estimateResponse,
                state.traceId,
                orderId,
                RideStatus.ORDER_CREATED,
                System.currentTimeMillis()
        );
        store.put(userId, updated);
        log.info("状态更新 | userId={} | status=ORDER_CREATED | orderId={}", userId, orderId);
    }

    /**
     * 清除指定用户的状态（订单完成或取消后调用）
     */
    public void clear(String userId) {
        RideState removed = store.remove(userId);
        if (removed != null) {
            log.info("状态清除 | userId={} | lastStatus={} | orderId={}",
                    userId, removed.status, removed.orderId);
        }
    }

    // ==================== 读操作 ====================

    /**
     * 获取指定用户的当前状态
     *
     * @param userId 用户标识
     * @return 当前状态，无记录返回 null
     */
    public RideState get(String userId) {
        return store.get(userId);
    }

    /**
     * 获取当前状态，如果不存在则抛出异常
     */
    public RideState getRequired(String userId) {
        RideState state = store.get(userId);
        if (state == null) {
            throw new IllegalStateException("未找到用户 " + userId + " 的打车记录，请先调用 estimate");
        }
        return state;
    }

    /**
     * 检查是否存在指定状态的记录
     */
    public boolean hasStatus(String userId, RideStatus status) {
        RideState state = store.get(userId);
        return state != null && state.status == status;
    }

    /**
     * 判断用户是否有进行中的订单（ESTIMATED / WAITING_CONFIRM）
     */
    public boolean hasActiveSession(String userId) {
        RideState state = store.get(userId);
        if (state == null) return false;
        return state.status == RideStatus.ESTIMATED
                || state.status == RideStatus.WAITING_CONFIRM;
    }

    // ==================== 内部类型 ====================

    /**
     * 用户打车过程的完整状态快照（不可变记录）
     */
    public record RideState(
            String userId,
            TaxiEstimateRequest estimateRequest,
            TaxiEstimateResponse estimateResponse,
            String traceId,
            String orderId,
            RideStatus status,
            long timestamp
    ) {
        /** 获取状态持续时长（毫秒） */
        public long ageMs() {
            return System.currentTimeMillis() - timestamp;
        }
    }
}
