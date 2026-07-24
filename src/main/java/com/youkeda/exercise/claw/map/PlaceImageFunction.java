package com.youkeda.exercise.claw.map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 地点图片搜索 LLM Function
 *
 * <p>将地点图片搜索能力以 LLM Function Calling 的方式暴露给 ReActAgentExecutor。
 * 注册函数名 {@code place_image_search}。
 *
 * <p><b>图片发送采用 stash-consume 模式：</b>
 * <ol>
 *   <li>{@link #execute(String)} 获取图片 bytes 并暂存到 {@link #pendingPlaceImages}</li>
 *   <li>返回简化的 JSON 给 LLM（不含图片 URL）</li>
 *   <li>ChatTool 通过 {@link #consumePendingPlaceImages()} 消费图片直接发送</li>
 * </ol>
 *
 * <p>LLM 只收到地点名称和描述，用自己的话生成文字介绍，不接触图片数据。
 */
@Component
public class PlaceImageFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(PlaceImageFunction.class);

    private final PlaceImageService placeImageService;
    private final LLMFunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper;

    /** 暂存待发送的地点图片（stash-consume 模式） */
    private volatile List<PendingPlaceImage> pendingPlaceImages;

    public PlaceImageFunction(PlaceImageService placeImageService,
                              LLMFunctionRegistry functionRegistry,
                              ObjectMapper objectMapper) {
        this.placeImageService = placeImageService;
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("PlaceImageFunction 已注册: place_image_search (stash-consume 模式)");
    }

    // ==================== Stash-Consume API ====================

    /**
     * 暂存的地点图片记录
     */
    public record PendingPlaceImage(byte[] imageBytes, String placeName) {}

    /**
     * 消费并清除暂存的地点图片。
     * ChatTool 在 stash-consume 阶段调用。
     *
     * @return 暂存的图片列表，无暂存数据时返回 null
     */
    public List<PendingPlaceImage> consumePendingPlaceImages() {
        List<PendingPlaceImage> images = pendingPlaceImages;
        pendingPlaceImages = null;
        return images;
    }

    // ==================== LLMFunction ====================

    @Override
    public String getName() {
        return "place_image_search";
    }

    @Override
    public String getDescription() {
        return "根据地点关键词和城市搜索地点相关图片。" +
                "当用户询问某地有哪些景点、团建场所、旅游目的地并希望看到图片时调用。" +
                "调用后你会收到地点名称和描述，请用自然语言向用户介绍该地点，" +
                "不需要在回复中嵌入任何图片URL或JSON格式。图片会自动发送给用户。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");

        ObjectNode keyword = properties.putObject("keyword");
        keyword.put("type", "string");
        keyword.put("description", "地点关键词，如：西湖、灵隐寺、团建基地、户外拓展");

        ObjectNode city = properties.putObject("city");
        city.put("type", "string");
        city.put("description", "城市名称，如：杭州、上海、北京");

        params.putArray("required").add("keyword");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String keyword = args.path("keyword").asText("");
            String city = args.path("city").asText("");

            if (keyword.isBlank()) {
                return errorResult("缺少必填参数: keyword");
            }

            log.info("PlaceImageFunction 执行 | keyword={} | city={}", keyword, city);

            // 获取图片字节
            List<byte[]> imageBytes = placeImageService.searchImageBytes(keyword, city);

            // stash 图片字节（ChatTool 消费）
            if (imageBytes != null && !imageBytes.isEmpty()) {
                List<PendingPlaceImage> pending = new ArrayList<>();
                for (byte[] bytes : imageBytes) {
                    if (bytes != null && bytes.length > 0) {
                        pending.add(new PendingPlaceImage(bytes, keyword));
                    }
                }
                if (!pending.isEmpty()) {
                    pendingPlaceImages = pending;
                }
            }

            // 返回给 LLM 的简洁信息（不含图片 URL、不含 _hint）
            ObjectNode result = objectMapper.createObjectNode();
            result.put("name", keyword);
            result.put("city", city != null ? city : "");
            result.put("description", generateDescription(keyword, city));
            int imgCount = (imageBytes != null) ? imageBytes.size() : 0;
            result.put("imageCount", imgCount);
            result.put("imagesSent", imgCount > 0);

            log.info("PlaceImageFunction 完成 | keyword={} | city={} | imagesStashed={}",
                    keyword, city,
                    (pendingPlaceImages != null) ? pendingPlaceImages.size() : 0);
            return result.toString();

        } catch (Exception e) {
            log.error("PlaceImageFunction 执行失败 | args={} | error={}",
                    argumentsJson, e.getMessage());
            return errorResult("地点图片搜索失败：" + e.getMessage());
        }
    }

    /**
     * 生成地点简介
     */
    private String generateDescription(String keyword, String city) {
        String location = (city != null && !city.isBlank()) ? city : "";
        return location + "的" + keyword + "是一处值得探索的地点。";
    }

    private String errorResult(String message) {
        try {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "ERROR");
            result.put("error", message);
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return "{\"status\":\"ERROR\",\"error\":\"" + message + "\"}";
        }
    }
}
