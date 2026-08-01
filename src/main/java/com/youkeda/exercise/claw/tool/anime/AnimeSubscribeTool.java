package com.youkeda.exercise.claw.tool.anime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.Tool;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.feature.anime.client.AniListClient;
import com.youkeda.exercise.claw.domain.anime.Anime;
import com.youkeda.exercise.claw.feature.anime.store.AnimeSubscriptionStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "anime.enabled", havingValue = "true")
public class AnimeSubscribeTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(AnimeSubscribeTool.class);

    private final AniListClient aniListClient;
    private final AnimeSubscriptionStore subscriptionStore;
    private final ToolRegistry functionRegistry;
    private final ObjectMapper objectMapper;

    public AnimeSubscribeTool(AniListClient aniListClient,
                                  AnimeSubscriptionStore subscriptionStore,
                                  ToolRegistry functionRegistry,
                                  ObjectMapper objectMapper) {
        this.aniListClient = aniListClient;
        this.subscriptionStore = subscriptionStore;
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("AnimeSubscribeTool 已注册");
    }

    @Override
    public String getName() { return "anime_subscribe"; }

    @Override
    public String getDescription() {
        return "管理追番列表。支持搜索番剧、订阅、取消订阅、查看列表。"
            + "当用户已从推荐列表中指定番剧时，优先使用 animeId 参数进行 subscribe 操作。"
            + "当用户说'帮我追番'、'订阅'、'取消追番'、'我追的番'时调用。";
    }

    @Override
    public JsonNode getParameters() {
        var root = objectMapper.createObjectNode();
        root.put("type", "object");

        var properties = root.putObject("properties");

        var actionProp = properties.putObject("action");
        actionProp.put("type", "string");
        actionProp.put("description", "操作类型：search=搜索番剧, subscribe=订阅, unsubscribe=取消订阅, list=查看列表");
        actionProp.set("enum", objectMapper.createArrayNode()
            .add("search").add("subscribe").add("unsubscribe").add("list"));

        var nameProp = properties.putObject("animeName");
        nameProp.put("type", "string");
        nameProp.put("description", "番剧名称（search 时需要；subscribe/unsubscribe 时若提供 animeId 则非必填。注意：AniList 不支持中文搜索，请使用英文或罗马音标题搜索）");

        var idProp = properties.putObject("animeId");
        idProp.put("type", "number");
        idProp.put("description", "AniList 番剧 ID（subscribe 时推荐使用，比按名称搜索更准确，来自推荐列表中的 id 字段）");

        root.set("required", objectMapper.createArrayNode().add("action"));
        return root;
    }

    @Override
    public String execute(String argumentsJson) {
        return execute(argumentsJson, null);
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String action = args.path("action").asText("list");

            return switch (action) {
                case "search" -> handleSearch(args);
                case "subscribe" -> handleSubscribe(args);
                case "unsubscribe" -> handleUnsubscribe(args);
                case "list" -> handleList();
                default -> "{\"status\":\"ERROR\",\"message\":\"未知操作: " + action + "\"}";
            };
        } catch (Exception e) {
            log.error("AnimeSubscribeTool 执行失败", e);
            return "{\"status\":\"ERROR\",\"message\":\"执行失败: " + e.getMessage() + "\"}";
        }
    }

    private String handleSearch(JsonNode args) throws Exception {
        String keyword = args.path("animeName").asText("");
        if (keyword.isBlank()) {
            return "{\"status\":\"ERROR\",\"message\":\"请提供要搜索的番剧名称\"}";
        }
        List<Anime> results = aniListClient.searchAnime(keyword);
        if (results.isEmpty()) {
            return "{\"status\":\"SUCCESS\",\"message\":\"未找到与「" + keyword + "」相关的番剧\"}";
        }
        // 格式化搜索结果返回给 LLM
        return objectMapper.writeValueAsString(Map.of(
            "status", "SUCCESS",
            "results", results.stream().map(a -> Map.of(
                "id", a.getAnilistId(),
                "title", a.getTitle(),
                "status", a.getStatus()
            )).toList()
        ));
    }

    private String handleSubscribe(JsonNode args) throws Exception {
        int animeId = args.path("animeId").asInt(0);
        String name = args.path("animeName").asText("");

        Anime target = null;

        // 优先按 ID 直接查询（推荐列表返回了 id，LLM 应当使用此路径）
        if (animeId > 0) {
            target = aniListClient.getAnimeById(animeId);
            if (target == null) {
                return "{\"status\":\"ERROR\",\"message\":\"未找到 ID 为 " + animeId
                        + " 的番剧，或 AniList 查询失败，请稍后重试或提供番剧名称\"}";
            }
        } else if (!name.isBlank()) {
            // 兜底：按名称搜索。AniList 不支持中文搜索，中文名/API 异常均返回空结果。
            List<Anime> results = aniListClient.searchAnime(name);
            if (results.isEmpty()) {
                // 任何未完成订阅的情况都不能返回 SUCCESS，否则 LLM 会误报"已订阅"
                return "{\"status\":\"ERROR\",\"message\":\"未找到与「" + name
                        + "」相关的番剧（AniList 不支持中文搜索），请提供英文/罗马音标题或番剧 ID\"}";
            }
            target = results.get(0);
        } else {
            return "{\"status\":\"ERROR\",\"message\":\"请提供番剧 ID 或名称\"}";
        }

        // 数据库保存失败时必须返回 ERROR，不得让 LLM 误报订阅成功
        try {
            subscriptionStore.subscribe(target);
        } catch (Exception e) {
            log.error("保存订阅失败 | id={} | title={}", target.getAnilistId(), target.getTitle(), e);
            return "{\"status\":\"ERROR\",\"message\":\"订阅保存失败，请稍后重试\"}";
        }
        log.info("用户订阅了番剧 | title={} | id={}", target.getTitle(), target.getAnilistId());
        return "{\"status\":\"SUCCESS\",\"message\":\"已订阅《" + target.getTitle() + "》！播出前会提醒你。\"}";
    }

    private String handleUnsubscribe(JsonNode args) throws Exception {
        int animeId = args.path("animeId").asInt(0);
        String name = args.path("animeName").asText("");
        if (animeId > 0) {
            subscriptionStore.unsubscribe(animeId);
            return "{\"status\":\"SUCCESS\",\"message\":\"已取消订阅\"}";
        }
        // 按名称取消
        Anime subscribed = subscriptionStore.listAll().stream()
            .filter(a -> a.getTitle().contains(name))
            .findFirst().orElse(null);
        if (subscribed == null) {
            return "{\"status\":\"ERROR\",\"message\":\"未找到匹配的番剧\"}";
        }
        subscriptionStore.unsubscribe(subscribed.getAnilistId());
        return "{\"status\":\"SUCCESS\",\"message\":\"已取消订阅《" + subscribed.getTitle() + "》\"}";
    }

    private String handleList() throws Exception {
        List<Anime> list = subscriptionStore.listAll();
        if (list.isEmpty()) {
            return "{\"status\":\"SUCCESS\",\"message\":\"你还没有追任何番剧。说'帮我追咒术回战'来开始追番！\"}";
        }
        return objectMapper.writeValueAsString(Map.of(
            "status", "SUCCESS",
            "subscriptions", list.stream().map(a -> Map.of(
                "title", a.getTitle(),
                "status", a.getStatus(),
                "id", a.getAnilistId()
            )).toList()
        ));
    }
}
