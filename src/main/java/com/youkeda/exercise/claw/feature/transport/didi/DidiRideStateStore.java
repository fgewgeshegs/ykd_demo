package com.youkeda.exercise.claw.feature.transport.didi;

import com.youkeda.exercise.claw.feature.transport.didi.model.TaxiEstimateRequest;
import com.youkeda.exercise.claw.feature.transport.didi.model.TaxiEstimateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 滴滴打车单次状态存储
 *
 * <p>管理打车流程状态，确保 {@code estimate → confirm → create_order} 的有序流转。
 *
 * <p>状态机：
 * <pre>
 *   ESTIMATED        — 估价完成，等待用户确认
 *       ↓
 *   WAITING_CONFIRM  — 用户已确认（LLM 调用 confirm），等待创建订单
 *       ↓
 *   ORDER_CREATED    — 订单创建成功
 * </pre>
 */
@Component
public class DidiRideStateStore {

    private static final Logger log = LoggerFactory.getLogger(DidiRideStateStore.class);

    private volatile RideState state;

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
     * @param request 估价请求参数
     * @param response 估价响应结果
     * @throws IllegalStateException 如果已有进行中的订单
     */
    public synchronized void saveEstimate(TaxiEstimateRequest request, TaxiEstimateResponse response) {
        if (state != null && state.status == RideStatus.ORDER_CREATED) {
            throw new IllegalStateException("已有进行中的订单，无法重新估价");
        }

        state = new RideState(
                request,
                response,
                response != null ? response.getTraceId() : null,
                null, // orderId
                RideStatus.ESTIMATED,
                System.currentTimeMillis()
        );
        log.info("状态更新 | status=ESTIMATED | traceId={}",
                response != null ? response.getTraceId() : "null");
    }

    /**
     * 确认订单（用户确认价格后调用），将状态从 {@link RideStatus#ESTIMATED} 转为
     * {@link RideStatus#WAITING_CONFIRM}。
     *
     * @throws IllegalStateException 如果状态不是 ESTIMATED
     */
    public synchronized void confirmBooking() {
        if (state == null) {
            throw new IllegalStateException("未找到打车估价记录，请先调用 estimate");
        }
        if (state.status == RideStatus.ORDER_CREATED) {
            throw new IllegalStateException("订单已创建，无需重复确认");
        }
        if (state.status != RideStatus.ESTIMATED) {
            throw new IllegalStateException("当前状态不允许确认：期望 ESTIMATED，实际 " + state.status);
        }

        state = new RideState(
                state.estimateRequest,
                state.estimateResponse,
                state.traceId,
                state.orderId,
                RideStatus.WAITING_CONFIRM,
                System.currentTimeMillis()
        );
        log.info("状态更新 | status=WAITING_CONFIRM | traceId={}", state.traceId);
    }

    /**
     * 保存订单 ID，更新状态为 {@link RideStatus#ORDER_CREATED}。
     *
     * @param orderId 订单 ID
     * @throws IllegalStateException 如果状态不是 WAITING_CONFIRM
     */
    public synchronized void saveOrder(String orderId) {
        if (state == null) {
            throw new IllegalStateException("未找到打车估价记录，请先调用 estimate");
        }
        if (state.status != RideStatus.WAITING_CONFIRM) {
            throw new IllegalStateException("订单未获得用户确认：期望 WAITING_CONFIRM，实际 " + state.status
                    + "。请先等待用户确认再创建订单");
        }

        state = new RideState(
                state.estimateRequest,
                state.estimateResponse,
                state.traceId,
                orderId,
                RideStatus.ORDER_CREATED,
                System.currentTimeMillis()
        );
        log.info("状态更新 | status=ORDER_CREATED | orderId={}", orderId);
    }

    /**
     * 清除状态（订单完成或取消后调用）
     */
    public synchronized void clear() {
        if (state != null) {
            log.info("状态清除 | lastStatus={} | orderId={}", state.status, state.orderId);
            state = null;
        }
    }

    // ==================== 读操作 ====================

    /**
     * 获取当前状态
     *
     * @return 当前状态，无记录返回 null
     */
    public RideState get() {
        return state;
    }

    /**
     * 获取当前状态，如果不存在则抛出异常
     */
    public RideState getRequired() {
        if (state == null) {
            throw new IllegalStateException("未找到打车记录，请先调用 estimate");
        }
        return state;
    }

    // ==================== 内部类型 ====================

    /**
     * 打车过程的完整状态快照（不可变记录）
     */
    public record RideState(
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
