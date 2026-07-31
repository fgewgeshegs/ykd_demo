package com.youkeda.exercise.claw.transport.didi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.transport.didi.DidiRideStateStore.RideState;
import com.youkeda.exercise.claw.transport.didi.DidiRideStateStore.RideStatus;
import com.youkeda.exercise.claw.transport.didi.model.TaxiEstimateRequest;
import com.youkeda.exercise.claw.transport.didi.model.TaxiEstimateResponse;
import com.youkeda.exercise.claw.transport.didi.model.TaxiOrderRequest;
import com.youkeda.exercise.claw.transport.didi.model.TaxiOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 滴滴打车业务编排层
 *
 * <p>职责：
 * <ul>
 *   <li>参数校验与转换</li>
 *   <li>调用 {@link DidiMcpClient#callTool} 执行 MCP 工具（maps_textsearch、taxi_estimate 等）</li>
 *   <li>响应格式化为 LLM 可读的结构化 JSON</li>
 *   <li>通过 {@link DidiRideStateStore} 管理多轮状态</li>
 * </ul>
 *
 * <p>调用 {@code maps_textsearch} 获取坐标是 service 的内部职责，
 * LLM 只需传入地址名称，无需关心坐标获取细节。
 */
@Service
public class DidiRideService {

    private static final Logger log = LoggerFactory.getLogger(DidiRideService.class);

    private final DidiMcpClient mcpClient;
    private final DidiRideStateStore stateStore;
    private final ObjectMapper objectMapper;

    public DidiRideService(DidiMcpClient mcpClient,
                           DidiRideStateStore stateStore,
                           ObjectMapper objectMapper) {
        this.mcpClient = mcpClient;
        this.stateStore = stateStore;
        this.objectMapper = objectMapper;
    }

    // ==================== 估价 ====================

    /**
     * 打车估价（MCP 调用链：maps_textsearch → taxi_estimate）
     *
     * <p>流程：
     * <ol>
     *   <li>解析起点/终点地址</li>
     *   <li>内部调用 {@code maps_textsearch} 获取滴滴认可的经纬度坐标</li>
     *   <li>调用 {@code taxi_estimate} 获取各车型估价和 traceId</li>
     *   <li>结果存入 {@link DidiRideStateStore}，状态设为 {@link RideStatus#ESTIMATED}</li>
     *   <li>返回格式化 JSON 给 LLM</li>
     * </ol>
     *
     * @param args   LLM 传入的参数（origin_name, destination_name 等）
     * @return 格式化估价结果
     */
    public String estimate(JsonNode args) {
        String originName = args.path("origin_name").asText("");
        String destinationName = args.path("destination_name").asText("");

        // 1. 参数校验
        if (originName.isBlank()) {
            return errorJson("缺少必填参数: origin_name（出发地名称）");
        }
        if (destinationName.isBlank()) {
            return errorJson("缺少必填参数: destination_name（目的地名称）");
        }

        log.info("打车估价 | from={} | to={}", originName, destinationName);

        try {
            // 2. 调用 maps_textsearch 获取起点坐标（滴滴 MCP 要求坐标必须来自 maps_textsearch）
            String fromLng = args.path("origin_lng").asText("");
            String fromLat = args.path("origin_lat").asText("");
            if (fromLng.isBlank() || fromLat.isBlank()) {
                log.info("调用 maps_textsearch 获取起点坐标 | query={}", originName);
                JsonNode originGeo = callMapsTextsearch(originName);
                fromLng = extractLng(originGeo);
                fromLat = extractLat(originGeo);
                log.info("起点坐标获取成功 | name={} | lng={} | lat={}", originName, fromLng, fromLat);
            }

            // 3. 调用 maps_textsearch 获取终点坐标
            String toLng = args.path("destination_lng").asText("");
            String toLat = args.path("destination_lat").asText("");
            if (toLng.isBlank() || toLat.isBlank()) {
                log.info("调用 maps_textsearch 获取终点坐标 | query={}", destinationName);
                JsonNode destGeo = callMapsTextsearch(destinationName);
                toLng = extractLng(destGeo);
                toLat = extractLat(destGeo);
                log.info("终点坐标获取成功 | name={} | lng={} | lat={}", destinationName, toLng, toLat);
            }

            // 4. 调用 taxi_estimate
            Map<String, Object> estimateArgs = new LinkedHashMap<>();
            estimateArgs.put("from_lng", fromLng);
            estimateArgs.put("from_lat", fromLat);
            estimateArgs.put("from_name", originName);
            estimateArgs.put("to_lng", toLng);
            estimateArgs.put("to_lat", toLat);
            estimateArgs.put("to_name", destinationName);

            log.info("调用 taxi_estimate | from={},{} | to={},{}", fromLng, fromLat, toLng, toLat);
            JsonNode estimateResult = mcpClient.callToolWithTextResult("taxi_estimate", estimateArgs);
            log.debug("taxi_estimate 响应 | result={}", estimateResult);

            // 5. 解析响应（支持 structuredContent 嵌套和扁平两种格式）
            JsonNode sc = estimateResult.get("structuredContent");
            JsonNode data = sc != null ? sc : estimateResult;

            String traceId = data.path("traceId").asText("");
            if (traceId.isBlank()) {
                log.warn("taxi_estimate 返回空 traceId | raw={}", estimateResult);
                return errorJson("估价失败：未获取到 traceId");
            }

            // 6. 构建响应模型
            TaxiEstimateRequest request = new TaxiEstimateRequest();
            request.setOriginName(originName);
            request.setOriginLng(fromLng);
            request.setOriginLat(fromLat);
            request.setDestName(destinationName);
            request.setDestLng(toLng);
            request.setDestLat(toLat);

            TaxiEstimateResponse response = parseEstimateResponse(traceId, data);

            // 7. 保存状态
            stateStore.saveEstimate(request, response);

            // 8. 返回格式化 JSON
            return formatEstimateResult(response, traceId, originName, destinationName);

        } catch (DidiMcpException e) {
            log.error("打车估价失败 | error={}", e.getMessage());
            return errorJson("打车估价失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("打车估价异常 | error={}", e.getMessage(), e);
            return errorJson("打车估价异常：" + e.getMessage());
        }
    }

    // ==================== 创建订单 ====================

    /**
     * 创建打车订单。
     *
     * <p>必须先调用 {@link #estimate} 并获得用户确认。
     * 状态机检查：ESTIMATED → WAITING_CONFIRM（自动确认）→ 调用 MCP → ORDER_CREATED。
     *
     * @param args   LLM 传入的参数（product_category 等）
     * @return 订单创建结果
     */
    public String createOrder(JsonNode args) {
        // 1. 检查状态：必须有 estimate 记录
        RideState state;
        try {
            state = stateStore.getRequired();
        } catch (IllegalStateException e) {
            return errorJson(e.getMessage());
        }

        // 2. 检查是否已创建
        if (state.status() == RideStatus.ORDER_CREATED) {
            return errorJson("订单已创建（orderId=" + state.orderId()
                    + "），请勿重复创建。如需重新叫车请先取消当前订单");
        }

        // 3. 状态路由
        if (state.status() == RideStatus.ESTIMATED) {
            // LLM 调用 create_order 即代表用户已在对话中确认
            // 自动完成 ESTIMATED → WAITING_CONFIRM 转型
            try {
                stateStore.confirmBooking();
                state = stateStore.get();
                log.info("用户确认打车 | traceId={}", state.traceId());
            } catch (IllegalStateException e) {
                return errorJson(e.getMessage());
            }
        }

        // WAITING_CONFIRM 检查：必须已经确认
        if (state.status() != RideStatus.WAITING_CONFIRM) {
            return errorJson("当前状态不允许创建订单：" + state.status()
                    + "。请先调用 estimate 并等待用户确认");
        }

        // 4. 提取参数
        String productCategory = args.path("product_category").asText("");
        if (productCategory.isBlank() && state.estimateResponse() != null) {
            // 容错：LLM 未传时取第一个车型
            productCategory = state.estimateResponse().getFirstProductCategory();
        }
        if (productCategory.isBlank()) {
            return errorJson("缺少必填参数: product_category（车型，如\"快车\"）");
        }

        String callerCarPhone = args.path("caller_car_phone").asText("");

        log.info("创建订单 | productCategory={} | traceId={}", productCategory, state.traceId());

        try {
            // 5. 调用 taxi_create_order
            Map<String, Object> orderArgs = new LinkedHashMap<>();
            orderArgs.put("product_category", productCategory);
            orderArgs.put("estimate_trace_id", state.traceId());
            if (!callerCarPhone.isBlank()) {
                orderArgs.put("caller_car_phone", callerCarPhone);
            }

            JsonNode orderResult = mcpClient.callToolWithTextResult("taxi_create_order", orderArgs);
            log.debug("taxi_create_order 响应 | result={}", orderResult);

            // 6. 解析响应
            JsonNode orderSc = orderResult.get("structuredContent");
            JsonNode orderData = orderSc != null ? orderSc : orderResult;

            String orderId = orderData.path("orderId").asText("");
            if (orderId.isBlank()) {
                log.warn("taxi_create_order 返回空 orderId | raw={}", orderResult);
                return errorJson("创建订单失败：未获取到订单号");
            }

            String status = orderData.path("status").asText("created");

            // 7. 保存订单
            stateStore.saveOrder(orderId);

            // 8. 返回格式化结果
            TaxiOrderResponse response = new TaxiOrderResponse();
            response.setOrderId(orderId);
            response.setStatus(status);
            response.setFromName(state.estimateRequest() != null
                    ? state.estimateRequest().getOriginName() : "");
            response.setToName(state.estimateRequest() != null
                    ? state.estimateRequest().getDestName() : "");

            return formatOrderResult(response);

        } catch (DidiMcpException e) {
            log.error("创建订单失败 | error={}", e.getMessage());
            return errorJson("创建订单失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("创建订单异常 | error={}", e.getMessage(), e);
            return errorJson("创建订单异常：" + e.getMessage());
        }
    }

    // ==================== 查询订单 ====================

    /**
     * 查询订单状态
     */
    public String queryOrder(JsonNode args) {
        String orderId = resolveOrderId(args);
        if (orderId == null) {
            return errorJson("缺少 order_id，且未找到进行中的订单");
        }

        log.info("查询订单 | orderId={}", orderId);

        try {
            Map<String, Object> queryArgs = new LinkedHashMap<>();
            queryArgs.put("order_id", orderId);

            JsonNode result = mcpClient.callToolWithTextResult("taxi_query_order", queryArgs);
            log.debug("taxi_query_order 响应 | result={}", result);

            JsonNode sc = result.get("structuredContent");
            JsonNode data = sc != null ? sc : result;

            return formatQueryResult(data, orderId);

        } catch (DidiMcpException e) {
            log.error("查询订单失败 | orderId={} | error={}",
                    orderId, e.getMessage());
            return errorJson("查询订单失败：" + e.getMessage());
        }
    }

    // ==================== 取消订单 ====================

    /**
     * 取消订单
     */
    public String cancelOrder(JsonNode args) {
        String orderId = resolveOrderId(args);
        if (orderId == null) {
            return errorJson("缺少 order_id，且未找到进行中的订单");
        }

        log.info("取消订单 | orderId={}", orderId);

        try {
            Map<String, Object> cancelArgs = new LinkedHashMap<>();
            cancelArgs.put("order_id", orderId);

            JsonNode result = mcpClient.callToolWithTextResult("taxi_cancel_order", cancelArgs);
            log.debug("taxi_cancel_order 响应 | result={}", result);

            // 清除状态
            stateStore.clear();

            ObjectNode output = objectMapper.createObjectNode();
            output.put("status", "cancelled");
            output.put("order_id", orderId);
            output.put("message", "订单 " + orderId + " 已取消");
            return objectMapper.writeValueAsString(output);

        } catch (Exception e) {
            log.error("取消订单失败 | orderId={} | error={}",
                    orderId, e.getMessage());
            return errorJson("取消订单失败：" + e.getMessage());
        }
    }

    // ==================== 生成跳转链接 ====================

    /**
     * 生成跳转滴滴 App/小程序的深度链接
     *
     * <p>与 estimate 一样，需要先通过 maps_textsearch 获取坐标，
     * 因为 taxi_generate_ride_app_link 要求传入经纬度坐标。
     */
    public String generateLink(JsonNode args) {
        String originName = args.path("origin_name").asText("");
        String destinationName = args.path("destination_name").asText("");

        if (originName.isBlank() || destinationName.isBlank()) {
            return errorJson("生成跳转链接需要 origin_name 和 destination_name");
        }

        log.info("生成跳转链接 | from={} | to={}", originName, destinationName);

        try {
            // 1. 获取起点坐标
            log.info("调用 maps_textsearch 获取起点坐标 | query={}", originName);
            JsonNode originGeo = callMapsTextsearch(originName);
            String fromLng = extractLng(originGeo);
            String fromLat = extractLat(originGeo);

            // 2. 获取终点坐标
            log.info("调用 maps_textsearch 获取终点坐标 | query={}", destinationName);
            JsonNode destGeo = callMapsTextsearch(destinationName);
            String toLng = extractLng(destGeo);
            String toLat = extractLat(destGeo);

            // 3. 调用 taxi_generate_ride_app_link（携带坐标）
            Map<String, Object> linkArgs = new LinkedHashMap<>();
            linkArgs.put("from_name", originName);
            linkArgs.put("from_lng", fromLng);
            linkArgs.put("from_lat", fromLat);
            linkArgs.put("to_name", destinationName);
            linkArgs.put("to_lng", toLng);
            linkArgs.put("to_lat", toLat);

            JsonNode result = mcpClient.callToolWithTextResult("taxi_generate_ride_app_link", linkArgs);
            log.debug("taxi_generate_ride_app_link 响应 | result={}", result);

            ObjectNode output = objectMapper.createObjectNode();
            output.put("status", "success");
            output.put("link_type", "didi_app_link");
            output.put("origin_name", originName);
            output.put("destination_name", destinationName);
            output.set("data", result);
            return objectMapper.writeValueAsString(output);

        } catch (Exception e) {
            log.warn("生成跳转链接失败 | error={}", e.getMessage());
            return errorJson("生成跳转链接失败：" + e.getMessage()
                    + "。可提示用户自行打开滴滴 App 叫车");
        }
    }

    // ==================== 内部工具 ====================

    /**
     * 调用 MCP maps_textsearch 获取地址坐标
     *
     * <p>maps_textsearch 需要 keywords（关键词）+ city（城市）两个必填参数。
     * 代码从 originName/destinationName 中自动提取城市名。
     */
    private JsonNode callMapsTextsearch(String query) {
        Map<String, Object> geoArgs = new LinkedHashMap<>();
        geoArgs.put("keywords", query);
        String city = extractCity(query);
        if (!city.isBlank()) {
            geoArgs.put("city", city);
        }
        return mcpClient.callToolWithTextResult("maps_textsearch", geoArgs);
    }

    /**
     * 从地址字符串中提取城市名
     *
     * <p>maps_textsearch 的 city 参数为必填，此方法尝试从地址前缀中提取城市名。
     * 例如："杭州余杭区阿里巴巴高桥云港" → "杭州"、"北京市天安门" → "北京市"。
     * 未匹配到已知城市时返回空字符串，不阻塞调用。
     */
    private String extractCity(String query) {
        if (query == null || query.isBlank()) return "";
        // 常见城市名列表（长名优先，避免"北京"误配"北京市"前缀给完整名）
        String[] cities = {
                "北京市", "上海市", "广州市", "深圳市", "杭州市", "成都市",
                "武汉市", "南京市", "重庆市", "天津市", "苏州市", "西安市",
                "长沙市", "郑州市", "东莞市", "青岛市", "沈阳市", "宁波市", "昆明市",
                "大连市", "厦门市", "合肥市", "佛山市", "福州市", "哈尔滨市", "济南市",
                "温州市", "长春市", "石家庄市", "常州市", "泉州市", "南宁市", "贵阳市",
                "南昌市", "太原市", "烟台市", "嘉兴市", "南通市", "金华市", "珠海市",
                "惠州市", "徐州市", "海口市", "乌鲁木齐市", "绍兴市", "中山市", "台州市",
                "兰州市", "北京", "上海", "广州", "深圳", "杭州", "成都",
                "武汉", "南京", "重庆", "天津", "苏州", "西安"
        };
        for (String city : cities) {
            if (query.startsWith(city)) {
                return city;
            }
        }
        return "";
    }

    /**
     * 从 maps_textsearch 响应中提取经度
     *
     * <p>支持以下响应格式：
     * <ul>
     *   <li>数组：取第一个元素，递归提取（maps_textsearch 默认返回 POI 列表）</li>
     *   <li>对象：直接匹配 lng/longitude/location.lng 等字段</li>
     * </ul>
     */
    private String extractLng(JsonNode geoResult) {
        // 处理数组：maps_textsearch 返回 [{location:{lng,lat}}, ...]
        if (geoResult.isArray() && geoResult.size() > 0) {
            return extractLng(geoResult.get(0));
        }
        // 尝试多种可能的字段路径
        if (geoResult.has("lng")) return geoResult.get("lng").asText("");
        if (geoResult.has("longitude")) return geoResult.get("longitude").asText("");
        if (geoResult.has("location")) {
            JsonNode loc = geoResult.get("location");
            if (loc.has("lng")) return loc.get("lng").asText("");
            if (loc.has("longitude")) return loc.get("longitude").asText("");
        }
        if (geoResult.has("result")) {
            JsonNode r = geoResult.get("result");
            if (r.has("location")) {
                JsonNode loc = r.get("location");
                if (loc.has("lng")) return loc.get("lng").asText("");
            }
        }
        log.warn("maps_textsearch 响应中未找到经度字段 | keys={}",
                joinFieldNames(geoResult));
        return "";
    }

    /**
     * 从 maps_textsearch 响应中提取纬度
     *
     * <p>支持以下响应格式：
     * <ul>
     *   <li>数组：取第一个元素，递归提取（maps_textsearch 默认返回 POI 列表）</li>
     *   <li>对象：直接匹配 lat/latitude/location.lat 等字段</li>
     * </ul>
     */
    private String extractLat(JsonNode geoResult) {
        // 处理数组：maps_textsearch 返回 [{location:{lng,lat}}, ...]
        if (geoResult.isArray() && geoResult.size() > 0) {
            return extractLat(geoResult.get(0));
        }
        if (geoResult.has("lat")) return geoResult.get("lat").asText("");
        if (geoResult.has("latitude")) return geoResult.get("latitude").asText("");
        if (geoResult.has("location")) {
            JsonNode loc = geoResult.get("location");
            if (loc.has("lat")) return loc.get("lat").asText("");
            if (loc.has("latitude")) return loc.get("latitude").asText("");
        }
        if (geoResult.has("result")) {
            JsonNode r = geoResult.get("result");
            if (r.has("location")) {
                JsonNode loc = r.get("location");
                if (loc.has("lat")) return loc.get("lat").asText("");
            }
        }
        log.warn("maps_textsearch 响应中未找到纬度字段 | keys={}",
                joinFieldNames(geoResult));
        return "";
    }

    /**
     * 拼接 JsonNode 的字段名为逗号分隔字符串
     */
    private static String joinFieldNames(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(it.next());
        }
        return sb.toString();
    }

    /**
     * 解析 taxi_estimate 响应中的 items 列表
     */
    private TaxiEstimateResponse parseEstimateResponse(String traceId, JsonNode data) {
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
    private String formatEstimateResult(TaxiEstimateResponse response, String traceId,
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
    private String formatOrderResult(TaxiOrderResponse response) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("status", "order_created");
            root.put("order_id", response.getOrderId());
            root.put("order_status", response.getStatus());
            root.put("from", response.getFromName());
            root.put("to", response.getToName());
            root.put("message", "订单已创建，请告知用户订单号和预计等待时间");
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            log.error("格式化订单结果失败", e);
            return "{\"status\":\"order_created\",\"orderId\":\"" + response.getOrderId() + "\"}";
        }
    }

    /**
     * 格式化查询订单结果
     */
    private String formatQueryResult(JsonNode data, String orderId) {
        try {
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
     * 解析 order_id 参数：优先从 args 中取，其次从 StateStore 中恢复
     */
    private String resolveOrderId(JsonNode args) {
        // 优先 LLM 传入
        String orderId = args.path("order_id").asText("");
        if (!orderId.isBlank()) {
            return orderId;
        }

        // 从状态存储中恢复
        RideState state = stateStore.get();
        if (state != null && state.orderId() != null) {
            return state.orderId();
        }

        return null;
    }

    /**
     * 构建错误 JSON
     */
    private String errorJson(String message) {
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
