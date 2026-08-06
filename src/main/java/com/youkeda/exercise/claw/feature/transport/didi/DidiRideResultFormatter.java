package com.youkeda.exercise.claw.feature.transport.didi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.feature.transport.didi.model.TaxiEstimateResponse;
import com.youkeda.exercise.claw.feature.transport.didi.model.TaxiOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 滴滴打车结果格式化服务。
 *
 * <p>把 MCP 响应解析/格式化为 LLM 可读的结构化 JSON，
 * 并统一构建错误 JSON。纯函数式，无状态。
 */
@Service
public class DidiRideResultFormatter {

    private static final Logger log = LoggerFactory.getLogger(DidiRideResultFormatter.class);

    private final ObjectMapper objectMapper;

    public DidiRideResultFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析 taxi_estimate 响应中的 items 列表
     */
    public TaxiEstimateResponse parseEstimateResponse(String traceId, JsonNode data) {
        TaxiEstimateResponse response = new TaxiEstimateResponse();
        response.setTraceId(traceId);

        List<TaxiEstimateResponse.EstimateItem> items = new ArrayList<>();
        JsonNode itemsNode = data.get("items");
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                TaxiEstimateResponse.EstimateItem ei = new TaxiEstimateResponse.EstimateItem();
                ei.setProductName(item.path("productName").asText(""));
                ei.setProductCategory(item.path("productCategory").asText(""));
                ei.setPriceText(item.path("priceText").asText(""));
                ei.setDestTimeText(item.path("destTimeText").asText(""));
                items.add(ei);
            }
        }
        response.setItems(items);

        return response;
    }

    /**
     * 格式化估价结果为 LLM 可读的 JSON
     */
    public String formatEstimateResult(TaxiEstimateResponse response, String traceId,
                                       String originName, String destinationName) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("status", "estimate_completed");
            root.put("origin_name", originName);
            root.put("destination_name", destinationName);

            ArrayNode products = root.putArray("available_products");
            for (TaxiEstimateResponse.EstimateItem item : response.getItems()) {
                ObjectNode p = products.addObject();
                p.put("product_name", item.getProductName());
                p.put("price", item.getPriceText() + "元");
                if (item.getDestTimeText() != null && !item.getDestTimeText().isBlank()) {
                    p.put("estimated_time", item.getDestTimeText());
                }
                p.put("product_category", item.getProductCategory());
            }

            root.put("message", "已获取打车估价，请将以上价格展示给用户并获得确认后，再调用 create_order");
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            log.error("格式化估价结果失败", e);
            return "{\"status\":\"estimate_completed\",\"traceId\":\"" + traceId + "\"}";
        }
    }

    /**
     * 格式化订单创建结果为 LLM 可读的 JSON
     */
    public String formatOrderResult(TaxiOrderResponse response) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("status", "order_created");
            root.put("order_id", response.getOrderId());
            root.put("order_status", response.getStatus());
            root.put("from", response.getFromName());
            root.put("to", response.getToName());
            root.put("message", "订单 " + response.getOrderId() + " 已创建，正在匹配司机，请耐心等待");
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            log.error("格式化订单结果失败", e);
            return "{\"status\":\"order_created\",\"orderId\":\"" + response.getOrderId() + "\"}";
        }
    }

    /**
     * 格式化查询订单结果。
     *
     * <p>当 MCP 返回非 JSON 纯文本（如"系统正在为您匹配最近的司机..."）时，
     * 从文本关键词推断订单状态，原始文本保留到 message 字段供 LLM 展示。
     */
    public String formatQueryResult(JsonNode data, String orderId) {
        try {
            // 检测 MCP 返回纯文本包装（非 JSON）→ 关键词推断状态
            String rawText = data.path("text").asText(null);
            if (rawText != null && !rawText.isBlank()
                    && data.path("status").asText(null) == null) {
                return formatTextQueryResult(orderId, rawText);
            }

            ObjectNode root = objectMapper.createObjectNode();
            root.put("order_id", orderId);
            root.put("status", data.path("status").asText("unknown"));
            root.put("driver_name", data.path("driverName").asText(""));
            root.put("driver_phone", data.path("driverPhone").asText(""));
            root.put("car_number", data.path("carNumber").asText(""));

            // 司机位置（如 MCP 提供）
            JsonNode driverLoc = data.get("driverLocation");
            if (driverLoc != null) {
                root.set("driver_location", driverLoc);
            }

            // 行程进度
            if (data.has("progress")) {
                root.put("progress", data.path("progress").asText(""));
            }

            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            log.error("格式化查询结果失败", e);
            return "{\"order_id\":\"" + orderId + "\",\"status\":\"queried\"}";
        }
    }

    /**
     * 从 MCP 返回的非 JSON 纯文本中推断订单状态。
     * 关键词按优先级匹配，命中即返回。
     */
    private String formatTextQueryResult(String orderId, String text) {
        String status = inferStatus(text);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("order_id", orderId);
        root.put("status", status);
        root.put("message", text);

        log.info("MCP 文本订单状态推断 | orderId={} | status={} | text={}",
                orderId, status, text.length() > 60 ? text.substring(0, 60) + "..." : text);
        return root.toString();
    }

    /** 关键词 → 订单状态映射，按匹配优先级排列 */
    static String inferStatus(String text) {
        if (text == null || text.isBlank()) return "unknown";

        // 行程已完成（"到达目的地"优先于单独的"到达"）
        if (containsAny(text, "完成", "到达目的地", "finished", "completed")) return "completed";
        // 司机已到达上车点（"已到达""到达"在 didi 语境中表示到达上车点而非目的地）
        if (containsAny(text, "已到达", "到达", "等待上车", "arrived", "waiting")) return "arrived";
        // 进行中（行程中）
        if (containsAny(text, "进行中", "in progress", "started")) return "in_progress";
        // 司机已接单/已接驾
        if (containsAny(text, "已接单", "已接驾", "accepted")) return "accepted";
        // 司机正在赶来
        if (containsAny(text, "前往", "赶来", "en route", "coming")) return "en_route";
        // 正在匹配/寻找司机（在 cancelled 之前检查，避免"可随时取消订单"提示语误判）
        if (containsAny(text, "匹配", "寻找", "等待司机", "searching")) return "searching";
        // 订单已取消（精确匹配，避免匹配"可取消""随时取消"等提示语）
        if (containsAny(text, "已取消", "订单取消", "cancelled")) return "cancelled";

        // 无法识别
        return "unknown";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    /**
     * 构建错误 JSON
     */
    public String errorJson(String message) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("status", "error");
            root.put("error", message);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"error\":\"" + message.replace("\"", "'") + "\"}";
        }
    }
}
