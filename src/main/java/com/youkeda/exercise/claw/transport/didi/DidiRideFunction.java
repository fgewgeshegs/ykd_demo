package com.youkeda.exercise.claw.transport.didi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 滴滴打车 LLM Function
 *
 * <p>将滴滴 MCP 打车能力以 LLM Function Calling 的方式暴露给 ReActAgentExecutor。
 * 注册函数名 {@code didi_taxi}，是一个统一的滴滴领域入口。
 *
 * <p>支持的 action：
 * <ul>
 *   <li><b>estimate</b> — 查询打车费用。根据起点/终点地址获取各车型估价。</li>
 *   <li><b>create_order</b> — 创建订单。必须先完成 estimate 并获得用户确认。</li>
 *   <li><b>query_order</b> — 查询订单状态/司机信息。</li>
 *   <li><b>cancel_order</b> — 取消订单。</li>
 *   <li><b>generate_link</b> — 生成跳转滴滴 App/小程序的深度链接。</li>
 * </ul>
 *
 * <p>核心调用链：
 * <pre>
 * LLM → DidiRideFunction → DidiRideService → DidiMcpClient → 滴滴 MCP Server
 * </pre>
 */
@Component
public class DidiRideFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(DidiRideFunction.class);

    private final DidiRideService rideService;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry functionRegistry;

    public DidiRideFunction(DidiRideService rideService,
                            ObjectMapper objectMapper,
                            LLMFunctionRegistry functionRegistry) {
        this.rideService = rideService;
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("DidiRideFunction 已注册到 LLMFunctionRegistry（didi_taxi）");
    }

    @Override
    public String getName() {
        return "didi_taxi";
    }

    @Override
    public String getDescription() {
        return "滴滴打车能力。当用户选择打车方式前往车站、机场、景点等目的地时调用。\n"
                + "支持以下操作：\n"
                + "1. estimate — 查询打车费用。根据起点和目的地地址获取各车型（快车、优享、专车等）的预估价格和时间。\n"
                + "2. create_order — 创建订单。注意：创建订单前必须先调用 estimate，"
                + "并将估价结果展示给用户，获得用户明确确认后才能创建订单。不得在用户确认前自动创建。\n"
                + "3. query_order — 查询订单状态和司机信息。\n"
                + "4. cancel_order — 取消已有订单。\n"
                + "5. generate_link — 生成跳转滴滴 App/小程序的链接。\n"
                + "坐标由系统自动处理，传入地址名称即可，用户无需提供经纬度。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");

        // action 枚举
        ObjectNode action = properties.putObject("action");
        action.put("type", "string");
        action.put("description", "操作类型：estimate（估价）、create_order（创建订单）、"
                + "query_order（查询订单）、cancel_order（取消订单）、generate_link（生成跳转链接）");
        ArrayNode actionEnum = action.putArray("enum");
        actionEnum.add("estimate");
        actionEnum.add("create_order");
        actionEnum.add("query_order");
        actionEnum.add("cancel_order");
        actionEnum.add("generate_link");

        // 通用参数
        ObjectNode originName = properties.putObject("origin_name");
        originName.put("type", "string");
        originName.put("description", "出发地名称，如：北京南站、天安门广场、我的当前位置。estimate 和 generate_link 时使用");

        ObjectNode destinationName = properties.putObject("destination_name");
        destinationName.put("type", "string");
        destinationName.put("description", "目的地名称，如：首都国际机场、西湖。estimate 和 generate_link 时使用");

        // 创建订单参数
        ObjectNode productCategory = properties.putObject("product_category");
        productCategory.put("type", "string");
        productCategory.put("description", "车型标识，来自 estimate 返回的 product_category。"
                + "如：快车、优享、专车、豪华车。create_order 时使用（如不传则默认第一项）");

        // 查询/取消订单参数
        ObjectNode orderId = properties.putObject("order_id");
        orderId.put("type", "string");
        orderId.put("description", "订单 ID。query_order 和 cancel_order 时使用，不传则自动使用最近订单");

        // 叫车人手机号
        ObjectNode callerCarPhone = properties.putObject("caller_car_phone");
        callerCarPhone.put("type", "string");
        callerCarPhone.put("description", "叫车人手机号（可选），create_order 时使用");

        params.putArray("required").add("action");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String action = args.path("action").asText("");

            if (action.isBlank()) {
                return "{\"status\":\"error\",\"error\":\"缺少必填参数: action（操作类型）\"}";
            }

            log.info("DidiRideFunction 执行 | action={} | args={}", action, args);

            return switch (action) {
                case "estimate" -> rideService.estimate("", args);
                case "create_order" -> rideService.createOrder("", args);
                case "query_order" -> rideService.queryOrder("", args);
                case "cancel_order" -> rideService.cancelOrder("", args);
                case "generate_link" -> rideService.generateLink("", args);
                default ->
                    "{\"status\":\"error\",\"error\":\"不支持的 action: " + action
                            + "，支持的 action: estimate, create_order, query_order, cancel_order, generate_link\"}";
            };

        } catch (Exception e) {
            log.error("DidiRideFunction 执行失败 | args={} | error={}", argumentsJson, e.getMessage());
            return "{\"status\":\"error\",\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        String result = execute(argumentsJson);

        // 如果 userId 不为空，替换空字符串为实际 userId
        if (context != null && context.userId() != null && !context.userId().isBlank()) {
            try {
                JsonNode args = objectMapper.readTree(argumentsJson);
                String action = args.path("action").asText("");
                String userId = context.userId();

                log.info("DidiRideFunction 带上下文执行 | userId={} | action={}", userId, action);

                return switch (action) {
                    case "estimate" -> rideService.estimate(userId, args);
                    case "create_order" -> rideService.createOrder(userId, args);
                    case "query_order" -> rideService.queryOrder(userId, args);
                    case "cancel_order" -> rideService.cancelOrder(userId, args);
                    case "generate_link" -> rideService.generateLink(userId, args);
                    default -> result; // 错误由无上下文的 execute 返回
                };
            } catch (Exception e) {
                log.error("DidiRideFunction 执行失败 | error={}", e.getMessage());
                return "{\"status\":\"error\",\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
            }
        }

        return result;
    }
}
