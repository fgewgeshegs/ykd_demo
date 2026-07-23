package com.youkeda.exercise.claw.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.map.model.DistanceRequest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯地图 LLM Function 入口
 *
 * <p>将腾讯地图能力以 LLM Function Calling 的方式暴露给 ReActAgentExecutor。
 * 注册三个函数：
 * <ul>
 *   <li>{@code map_search_place} — 地点搜索</li>
 *   <li>{@code map_route_planning} — 路线规划</li>
 *   <li>{@code map_distance_calculate} — 距离计算</li>
 * </ul>
 *
 * <p>函数注册后自动被 {@link LLMFunctionRegistry} 管理，
 * ReActAgentExecutor 在 tool-calling 循环中自动发现并调用。
 */
@Component
public class TencentMapFunction {

    private static final Logger log = LoggerFactory.getLogger(TencentMapFunction.class);

    private final MapService mapService;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry functionRegistry;

    public TencentMapFunction(MapService mapService,
                               ObjectMapper objectMapper,
                               LLMFunctionRegistry functionRegistry) {
        this.mapService = mapService;
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
    }

    @PostConstruct
    public void init() {
        // ==================== 1. 地点搜索 ====================
        functionRegistry.register(new LLMFunction() {
            @Override
            public String getName() {
                return "map_search_place";
            }

            @Override
            public String getDescription() {
                return "搜索指定地点，根据关键词和位置查找附近的景点、餐厅、酒店、活动场地等POI信息。" +
                        "适合用户问「附近有什么」或「推荐团建地点」时优先使用。返回结构化状态、地址和距离。" +
                        "如果状态为 EMPTY、PARTIAL 或 ERROR，再按 missing_information 使用 web_search 补充。";
            }

            @Override
            public JsonNode getParameters() {
                ObjectNode params = objectMapper.createObjectNode();
                params.put("type", "object");

                ObjectNode properties = params.putObject("properties");

                ObjectNode keyword = properties.putObject("keyword");
                keyword.put("type", "string");
                keyword.put("description", "搜索关键词，如：团建基地、餐厅、景点、酒店、户外拓展等");

                ObjectNode location = properties.putObject("location");
                location.put("type", "string");
                location.put("description", "位置，城市名称或区域名，如：无锡、上海、北京");

                params.putArray("required").add("keyword");

                return params;
            }

            @Override
            public String execute(String argumentsJson) {
                try {
                    JsonNode args = objectMapper.readTree(argumentsJson);
                    String keyword = args.path("keyword").asText("");
                    String location = args.path("location").asText("");

                    if (keyword.isBlank()) {
                        return errorResult("缺少必填参数: keyword");
                    }

                    log.info("TencentMapFunction.map_search_place | keyword={} | location={}", keyword, location);
                    String data = mapService.searchPlace(keyword, location);
                    if (data.contains("未找到")) {
                        return result("EMPTY", data, List.of("地点候选"), true);
                    }
                    return result("SUCCESS", data,
                            List.of("团建项目详情", "团队价格", "开放时间和预约政策"), true);

                } catch (Exception e) {
                    log.error("map_search_place 执行失败 | args={} | error={}", argumentsJson, e.getMessage());
                    return errorResult("地点搜索失败：" + e.getMessage());
                }
            }
        });

        // ==================== 2. 路线规划 ====================
        functionRegistry.register(new LLMFunction() {
            @Override
            public String getName() {
                return "map_route_planning";
            }

            @Override
            public String getDescription() {
                return "规划两个地点之间的路线。返回驾车距离、预计耗时和路线概览。" +
                        "适合用户问「怎么走」「怎么去」「路线」时使用。" +
                        "注意：参数地点名称会自动解析为坐标，无需事先调用地图编码。";
            }

            @Override
            public JsonNode getParameters() {
                ObjectNode params = objectMapper.createObjectNode();
                params.put("type", "object");

                ObjectNode properties = params.putObject("properties");

                ObjectNode origin = properties.putObject("origin");
                origin.put("type", "string");
                origin.put("description", "起点名称，如：无锡学院、拈花湾、灵山大佛");

                ObjectNode destination = properties.putObject("destination");
                destination.put("type", "string");
                destination.put("description", "终点名称，如：拈花湾、灵山大佛、鼋头渚");

                ObjectNode mode = properties.putObject("mode");
                mode.put("type", "string");
                mode.put("description", "出行方式，默认 driving（驾车）");
                ArrayNode enumValues = mode.putArray("enum");
                enumValues.add("driving");
                enumValues.add("walking");
                enumValues.add("transit");

                params.putArray("required").add("origin").add("destination");

                return params;
            }

            @Override
            public String execute(String argumentsJson) {
                try {
                    JsonNode args = objectMapper.readTree(argumentsJson);
                    String origin = args.path("origin").asText("");
                    String destination = args.path("destination").asText("");
                    String mode = args.path("mode").asText("driving");

                    if (origin.isBlank()) {
                        return errorResult("缺少必填参数: origin");
                    }
                    if (destination.isBlank()) {
                        return errorResult("缺少必填参数: destination");
                    }

                    log.info("TencentMapFunction.map_route_planning | origin={} | destination={} | mode={}",
                            origin, destination, mode);
                    return result("SUCCESS", mapService.routePlanning(origin, destination, mode), List.of(), false);

                } catch (TencentMapException e) {
                    log.error("map_route_planning 执行失败 | args={} | error={}", argumentsJson, e.getMessage());
                    return errorResult("路线规划失败：" + e.getMessage());
                } catch (Exception e) {
                    log.error("map_route_planning 执行异常 | args={} | error={}", argumentsJson, e.getMessage());
                    return errorResult("路线规划异常：" + e.getMessage());
                }
            }
        });

        // ==================== 3. 距离计算 ====================
        functionRegistry.register(new LLMFunction() {
            @Override
            public String getName() {
                return "map_distance_calculate";
            }

            @Override
            public String getDescription() {
                return "计算从一个地点到多个目的地的驾车距离并比较。" +
                        "适合用户问「哪个近」「哪个远」「距离比较」时使用。" +
                        "返回各地距离和推荐最近的目的地。";
            }

            @Override
            public JsonNode getParameters() {
                ObjectNode params = objectMapper.createObjectNode();
                params.put("type", "object");

                ObjectNode properties = params.putObject("properties");

                ObjectNode origin = properties.putObject("origin");
                origin.put("type", "string");
                origin.put("description", "起点名称，如：无锡学院、酒店名称");

                ObjectNode destinations = properties.putObject("destinations");
                destinations.put("type", "array");
                destinations.put("description", "多个目的地名称列表");
                ObjectNode items = destinations.putObject("items");
                items.put("type", "string");

                params.putArray("required").add("origin").add("destinations");

                return params;
            }

            @Override
            public String execute(String argumentsJson) {
                try {
                    JsonNode args = objectMapper.readTree(argumentsJson);
                    String origin = args.path("origin").asText("");

                    List<String> destinations = new ArrayList<>();
                    JsonNode destArray = args.path("destinations");
                    if (destArray.isArray()) {
                        for (JsonNode item : destArray) {
                            String dest = item.asText("");
                            if (!dest.isBlank()) {
                                destinations.add(dest);
                            }
                        }
                    }

                    if (origin.isBlank()) {
                        return errorResult("缺少必填参数: origin");
                    }
                    if (destinations.isEmpty()) {
                        return errorResult("缺少必填参数: destinations");
                    }

                    log.info("TencentMapFunction.map_distance_calculate | origin={} | destinations={}",
                            origin, destinations);
                    String data = mapService.calculateDistance(new DistanceRequest(origin, destinations));
                    if (data.startsWith("计算失败")) {
                        return result("EMPTY", data, List.of("候选地点距离和路线"), true);
                    }
                    if (data.contains("距离计算失败")) {
                        return result("PARTIAL", data, List.of("部分候选地点距离"), true);
                    }
                    return result("SUCCESS", data, List.of(), false);

                } catch (TencentMapException e) {
                    log.error("map_distance_calculate 执行失败 | args={} | error={}", argumentsJson, e.getMessage());
                    return errorResult("距离计算失败：" + e.getMessage());
                } catch (Exception e) {
                    log.error("map_distance_calculate 执行异常 | args={} | error={}", argumentsJson, e.getMessage());
                    return errorResult("距离计算异常：" + e.getMessage());
                }
            }
        });

        log.info("TencentMapFunction 已注册 3 个 LLM Function: map_search_place, map_route_planning, map_distance_calculate");
    }

    private String result(String status, String data, List<String> missingInformation,
                          boolean fallbackRequired) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", status);
        result.put("source", "TENCENT_MAP");
        result.put("data", data);
        ArrayNode missing = result.putArray("missing_information");
        missingInformation.forEach(missing::add);
        result.put("fallback_required", fallbackRequired);
        return result.toString();
    }

    private String errorResult(String message) {
        return result("ERROR", message, List.of("地图信息"), true);
    }
}
