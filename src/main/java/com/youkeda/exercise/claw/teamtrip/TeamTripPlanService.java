package com.youkeda.exercise.claw.teamtrip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 团建方案需求、候选方案、成本结果和用户决策的状态服务。 */
@Service
public class TeamTripPlanService {

    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)\\s*天");
    private static final Pattern NIGHTS_PATTERN = Pattern.compile("(\\d+)\\s*晚");
    private static final Pattern MONTH_DAY_PATTERN = Pattern.compile("(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*[日号]");
    private static final Set<String> PREFERENCE_FIELDS = Set.of(
            "activity_preferences", "team_goal", "participant_profile",
            "transport_preference", "accommodation_preference",
            "meal_preferences", "special_requirements");

    private final TeamTripPlanStateStore stateStore;
    private final ObjectMapper objectMapper;

    public TeamTripPlanService(TeamTripPlanStateStore stateStore, ObjectMapper objectMapper) {
        this.stateStore = stateStore;
        this.objectMapper = objectMapper;
    }

    public ObjectNode handle(String userId, JsonNode args) {
        String action = text(args, "action");
        if ("reset".equals(action)) {
            stateStore.clear(userId);
            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "RESET");
            result.put("instruction", "旧方案、候选方案、成本和用户决策均已清除，请重新收集需求。");
            return result;
        }

        TeamTripPlanDraft draft = stateStore.get(userId);
        if (draft == null) draft = new TeamTripPlanDraft();
        if (args.has("option_count") && args.get("option_count").canConvertToInt()) {
            int optionCount = args.get("option_count").asInt();
            if (optionCount < 1 || optionCount > 5) {
                return saveError(userId, draft, "候选方案数量必须在1到5之间。");
            }
        }
        boolean materialChange = merge(draft, args);
        if (materialChange) {
            draft.setHolidayStatus("NOT_CALLED");
            draft.setLastHolidayResult(null);
            draft.setTransportStatus("NOT_CALLED");
            draft.setLastTransportResult(null);
            if (!draft.getOptions().isEmpty()) invalidateAllOptions(draft);
        }

        return switch (action) {
            case "save_options" -> saveOptions(userId, draft, args);
            case "select_option" -> selectOption(userId, draft, text(args, "selected_option_id"));
            case "combine_options" -> combineOptions(userId, draft, args);
            case "revise_option" -> reviseOption(userId, draft, args);
            case "budget_decision" -> handleBudgetDecision(userId, draft, args);
            case "revise" -> handleRevision(userId, draft, text(args, "feedback"));
            default -> collect(userId, draft);
        };
    }

    public TeamTripPlanDraft getDraft(String userId) {
        return stateStore.get(userId);
    }

    /**
     * 对必须等待用户输入的阶段直接生成回复。
     *
     * <p>这些内容都来自已保存的结构化状态，不需要再次把完整方案和费用明细发给 LLM。
     * 返回 null 表示当前阶段仍需要由 LLM 继续处理。</p>
     */
    public String renderWaitingReply(String userId) {
        TeamTripPlanDraft draft = stateStore.get(userId);
        if (draft == null) return null;

        return switch (draft.getStage()) {
            case "NEED_MORE_INFORMATION" -> renderMissingInformation(draft);
            case "AWAITING_OPTION_SELECTION" -> renderOptionSelection(draft);
            case "AWAITING_BUDGET_DECISION" -> renderBudgetDecision(draft);
            case "AWAITING_ADJUSTMENT_PREFERENCE" ->
                    "为了把所选方案调整到预算内，请告诉我你更希望优先保留哪一项："
                            + "住宿品质、团建活动、餐饮标准，还是交通便利性？";
            case "AWAITING_ADJUSTMENT_SELECTION" ->
                    "请从刚才列出的可调整项目中选择要修改的项目；收到你的选择后，我再重新核算费用。";
            case "AWAITING_REVISION_REASON" ->
                    "请告诉我你对当前方案哪里不满意，以及最希望保留或改变的内容，我会据此调整策略。";
            default -> null;
        };
    }

    /** 达到内部处理上限时直接给出当前可用方案，不要求用户确认是否继续。 */
    public String renderBestAvailableReply(String userId) {
        TeamTripPlanDraft draft = stateStore.get(userId);
        if (draft == null) return null;
        if (!draft.getOptions().isEmpty()) return renderOptionSelection(draft);
        if ("NEED_MORE_INFORMATION".equals(draft.getStage())) {
            return renderMissingInformation(draft);
        }
        return "我已保留当前方案需求，但部分地点或价格信息暂时无法核实。"
                + "当前无法确认的费用将作为待确认项，不会要求你重新提供已经确认的信息。";
    }

    /**
     * 构造用于最终展示的精简快照，只保留方案摘要和预算结论，不携带搜索原文及完整工具历史。
     */
    public String buildPresentationSnapshot(String userId) {
        TeamTripPlanDraft draft = stateStore.get(userId);
        if (draft == null) return "{}";

        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.put("stage", draft.getStage());
        snapshot.put("departure_city", draft.getDepartureCity());
        snapshot.put("participant_count", draft.getParticipantCount());
        snapshot.put("travel_date", draft.getTravelDate());
        snapshot.put("duration", draft.getDuration());
        snapshot.put("destination", draft.getDestination());
        if (draft.getBudgetTotal() != null) snapshot.put("budget_total", draft.getBudgetTotal());
        if (draft.getBudgetPerPerson() != null) {
            snapshot.put("budget_per_person", draft.getBudgetPerPerson());
        }
        snapshot.put("plan_mode", draft.getPlanMode());
        snapshot.set("priorities", objectMapper.valueToTree(draft.getPriorities()));

        ArrayNode options = snapshot.putArray("options");
        for (TeamTripPlanOption option : draft.getOptions()) {
            ObjectNode item = options.addObject();
            item.put("option_id", option.getOptionId());
            item.put("display_name", option.getDisplayName());
            item.put("positioning", option.getPositioning());
            item.put("highlights", option.getHighlights());
            item.put("itinerary_summary", option.getItinerarySummary());
            JsonNode cost = option.getCostResult();
            if (cost != null && !cost.isNull()) {
                ObjectNode summary = item.putObject("cost");
                copyNumber(cost, summary, "estimatedTotalMin", "estimated_total_min");
                copyNumber(cost, summary, "estimatedTotalMax", "estimated_total_max");
                copyNumber(cost, summary, "perPersonMin", "per_person_min");
                copyNumber(cost, summary, "perPersonMax", "per_person_max");
                copyNumber(cost, summary, "targetBudget", "target_budget");
                copyNumber(cost, summary, "overrunMin", "overrun_min");
                copyNumber(cost, summary, "overrunMax", "overrun_max");
                copyNumber(cost, summary, "overrunRateMax", "overrun_rate_max");
                summary.put("budget_status", cost.path("budgetStatus").asText("INDETERMINATE"));
                summary.set("missing_price_items", cost.path("missingPriceItems").deepCopy());
            }
        }
        return snapshot.toString();
    }

    /** 由执行器在节假日、地图、天气、交通、搜索和成本工具执行后回写流程状态。 */
    public void recordToolResult(String userId, String toolName, String resultJson) {
        TeamTripPlanDraft draft = stateStore.get(userId);
        if (draft == null) return;

        JsonNode result = parse(resultJson);
        String status = result.has("error")
                ? "ERROR"
                : result.path("status").asText("SUCCESS").toUpperCase();
        String previousStage = draft.getStage();
        if ("holiday_check".equals(toolName)) {
            draft.setHolidayStatus(status);
            draft.setLastHolidayResult(result.deepCopy());
            if (!isWaitingForUser(draft)) {
                advanceInitialResearch(draft);
            }
        } else if (toolName.startsWith("map_")) {
            if ("map_route_planning".equals(toolName) || "map_distance_calculate".equals(toolName)) {
                draft.setRouteStatus(status);
            } else {
                draft.setMapStatus(status);
            }
            if (!isWaitingForUser(draft)) {
                if ("READY_FOR_DATE".equals(previousStage)
                        || "READY_FOR_DATE_CONTEXT".equals(previousStage)) {
                    // 地图可与相对日期换算并行，但必须等明确日期写回后才能执行节假日检查。
                    draft.setStage("READY_FOR_DATE_CONTEXT");
                } else {
                    advanceInitialResearch(draft);
                }
            }
        } else if ("weather_query".equals(toolName)) {
            draft.setWeatherStatus(status);
            if (!isWaitingForUser(draft)) {
                draft.setStage(isInsufficient(status) ? "WEATHER_INSUFFICIENT" : "READY_FOR_TRANSPORT");
            }
        } else if ("transport_recommend".equals(toolName)) {
            draft.setTransportStatus(status);
            draft.setLastTransportResult(result.deepCopy());
            if (!isWaitingForUser(draft)) draft.setStage("TRANSPORT_READY");
        } else if ("web_search".equals(toolName)) {
            draft.setWebSearchStatus(status);
            if (!isWaitingForUser(draft)) {
                if ("MAP_INSUFFICIENT".equals(previousStage)) {
                    // 网页搜索作为地图降级结果，之后仍需完成天气和交通评估。
                    draft.setStage("MAP_READY");
                } else if ("WEATHER_INSUFFICIENT".equals(previousStage)) {
                    // 天气已做过专业查询并完成降级补证，继续交通评估。
                    draft.setStage("READY_FOR_TRANSPORT");
                } else {
                    draft.setStage("EVIDENCE_READY");
                }
            }
        } else if ("budget_calculator".equals(toolName)) {
            recordCostResult(draft, result);
        }
        stateStore.save(userId, draft);
    }

    private ObjectNode collect(String userId, TeamTripPlanDraft draft) {
        Map<String, String> missing = findMissing(draft);
        ObjectNode result = baseResult(draft);
        if (!missing.isEmpty()) {
            draft.setStage("NEED_MORE_INFORMATION");
            result.put("status", "NEED_MORE_INFORMATION");
            result.put("instruction", "只追问缺失信息并停止，不调用地图、天气、网页搜索或成本工具。");
            ArrayNode fields = result.putArray("missing_fields");
            ArrayNode questions = result.putArray("questions");
            missing.forEach((field, question) -> {
                fields.add(field);
                questions.add(question);
            });
        } else if (needsDateNormalization(draft.getTravelDate())) {
            draft.setStage("READY_FOR_DATE_CONTEXT");
            result.put("status", "READY_FOR_DATE_CONTEXT");
            ArrayNode nextTools = result.putArray("next_tools");
            nextTools.add("time_query");
            if ("NOT_CALLED".equals(draft.getMapStatus())) nextTools.add("map_search_place");
            result.put("instruction", "相对日期需要先用 time_query 换算并再次调用 team_trip_plan 写回明确日期；"
                    + "地图查询与日期换算互不依赖，可在同一轮并行执行并复用结果。");
        } else {
            draft.setStage("READY_FOR_CONTEXT");
            result.put("status", "READY_FOR_CONTEXT");
            ArrayNode nextTools = result.putArray("next_tools");
            nextTools.add("holiday_check");
            if ("NOT_CALLED".equals(draft.getMapStatus())) nextTools.add("map_search_place");
            result.put("instruction", "日期已明确。调用 holiday_check 检查调休与团建适宜度；"
                    + ("NOT_CALLED".equals(draft.getMapStatus())
                    ? "同时并行调用地图工具确认地点和路线；"
                    : "此前完成的地图结果继续复用，无需重复查询；")
                    + "两者完成后再依次查询天气和交通方案，最后补齐价格、保存候选方案并核算预算。");
            addOutputContract(result);
        }
        refreshState(result, draft);
        stateStore.save(userId, draft);
        return result;
    }

    private ObjectNode saveOptions(String userId, TeamTripPlanDraft draft, JsonNode args) {
        JsonNode optionNodes = args.get("options");
        if (optionNodes == null || !optionNodes.isArray() || optionNodes.isEmpty()) {
            return saveError(userId, draft, "save_options 至少需要一个候选方案。");
        }
        if (optionNodes.size() > 5) {
            return saveError(userId, draft, "候选方案最多保存5个，请保留差异最明显的方案。");
        }
        if (optionNodes.size() != draft.getOptionCount()) {
            return saveError(userId, draft, "当前应生成" + draft.getOptionCount()
                    + "个候选方案，实际收到" + optionNodes.size() + "个。");
        }

        List<TeamTripPlanOption> options = new ArrayList<>();
        for (JsonNode node : optionNodes) {
            String id = text(node, "option_id");
            if (id.isBlank()) return saveError(userId, draft, "每个候选方案都必须提供 option_id。");
            TeamTripPlanOption option = new TeamTripPlanOption();
            option.setOptionId(id);
            option.setDisplayName(defaultText(text(node, "display_name"), id));
            option.setPositioning(text(node, "positioning"));
            option.setHighlights(text(node, "highlights"));
            option.setItinerarySummary(text(node, "itinerary_summary"));
            options.add(option);
        }

        draft.setOptions(options);
        draft.setSelectedOptionId(null);
        draft.setOptionSetVersion(Math.max(1, draft.getOptionSetVersion() + 1));
        draft.setOptionDecisionStatus(OptionDecisionStatus.OPTIONS_GENERATED);
        draft.setBudgetDecisionStatus(BudgetDecisionStatus.NOT_REQUIRED);
        draft.setCostStatus("NOT_CALCULATED");
        draft.setLastCostResult(null);
        draft.setStage("OPTIONS_READY_FOR_COSTING");

        ObjectNode result = baseResult(draft);
        result.put("status", "OPTIONS_READY_FOR_COSTING");
        result.put("next_tool", "budget_calculator");
        result.put("instruction", "候选方案已保存。先补齐每个方案交通、住宿、餐饮、门票和活动的单价来源；"
                + "价格查询与 budget_calculator 不得同轮调用。价格齐备后一次批量核算全部方案。");
        addOutputContract(result);
        refreshState(result, draft);
        stateStore.save(userId, draft);
        return result;
    }

    private ObjectNode selectOption(String userId, TeamTripPlanDraft draft, String optionId) {
        TeamTripPlanOption selected = findOption(draft, optionId);
        if (selected == null) return saveError(userId, draft, "未找到候选方案：" + optionId);

        draft.getOptions().forEach(option ->
                option.setPlanStatus(option == selected ? "SELECTED" : "CANDIDATE"));
        draft.setSelectedOptionId(selected.getOptionId());
        draft.setOptionDecisionStatus(OptionDecisionStatus.OPTION_SELECTED);

        ObjectNode result = baseResult(draft);
        if (!"READY".equals(selected.getCostStatus())) {
            draft.setStage("COST_PARTIAL");
            result.put("status", "COST_PARTIAL");
            result.put("instruction", "已选择该方案，但费用尚未完整核算。先补齐 missingPriceItems 后重新调用 budget_calculator。");
        } else {
            evaluateSelectedBudget(draft, selected);
            result.put("status", draft.getStage());
            if ("AWAITING_BUDGET_DECISION".equals(draft.getStage())) {
                addBudgetQuestion(result, selected);
            } else {
                result.put("instruction", "方案已选择且预算比较完成，可以按 output_contract 输出最终确认版。");
            }
        }
        addOutputContract(result);
        refreshState(result, draft);
        stateStore.save(userId, draft);
        return result;
    }

    private ObjectNode combineOptions(String userId, TeamTripPlanDraft draft, JsonNode args) {
        JsonNode sourceIds = args.get("source_option_ids");
        if (sourceIds == null || !sourceIds.isArray() || sourceIds.size() < 2) {
            return saveError(userId, draft, "组合方案至少需要两个 source_option_ids。");
        }
        String id = defaultText(text(args, "option_id"), "COMBINED-" + (draft.getOptionSetVersion() + 1));
        TeamTripPlanOption combined = new TeamTripPlanOption();
        combined.setOptionId(id);
        combined.setDisplayName(defaultText(text(args, "display_name"), "组合方案"));
        combined.setPositioning(defaultText(text(args, "positioning"), "用户组合方案"));
        combined.setHighlights(text(args, "highlights"));
        combined.setItinerarySummary(text(args, "itinerary_summary"));
        combined.setPlanStatus("CANDIDATE");
        draft.getOptions().add(combined);
        draft.setSelectedOptionId(id);
        draft.setOptionDecisionStatus(OptionDecisionStatus.OPTION_COMBINING);
        draft.setCostStatus("STALE");
        draft.setStage("OPTION_REVISION_REQUIRED");

        ObjectNode result = baseResult(draft);
        result.put("status", "OPTION_COMBINING");
        result.put("instruction", "新组合方案不能直接相加旧方案金额。请重新校验路线，查询受影响项目价格，"
                + "然后只核算这个组合方案。");
        refreshState(result, draft);
        stateStore.save(userId, draft);
        return result;
    }

    private ObjectNode reviseOption(String userId, TeamTripPlanDraft draft, JsonNode args) {
        TeamTripPlanOption option = findOption(draft, text(args, "option_id"));
        if (option == null) return saveError(userId, draft, "未找到需要修改的候选方案。");

        option.setVersion(Math.max(1, option.getVersion()) + 1);
        if (hasText(args, "display_name")) option.setDisplayName(text(args, "display_name"));
        if (hasText(args, "positioning")) option.setPositioning(text(args, "positioning"));
        if (hasText(args, "highlights")) option.setHighlights(text(args, "highlights"));
        if (hasText(args, "itinerary_summary")) option.setItinerarySummary(text(args, "itinerary_summary"));
        option.setCostStatus("STALE");
        option.setCostResult(null);
        option.setPlanStatus("NEEDS_REVIEW");
        draft.setSelectedOptionId(option.getOptionId());
        draft.setCostStatus("STALE");
        draft.setStage("OPTION_REVISION_REQUIRED");

        ObjectNode result = baseResult(draft);
        result.put("status", "OPTION_REVISION_REQUIRED");
        result.put("instruction", "只重新查询该方案受影响项目的地点或价格，再用新 plan_version 重新核算；其他方案保持不变。");
        refreshState(result, draft);
        stateStore.save(userId, draft);
        return result;
    }

    private ObjectNode handleBudgetDecision(String userId, TeamTripPlanDraft draft, JsonNode args) {
        TeamTripPlanOption selected = findOption(draft, draft.getSelectedOptionId());
        if (selected == null) return saveError(userId, draft, "请先选择一个候选方案。");

        String decision = text(args, "budget_decision");
        ObjectNode result = baseResult(draft);
        switch (decision) {
            case "ACCEPT_OVERRUN" -> {
                JsonNode cost = selected.getCostResult();
                draft.setAcceptedOverrunAmount(number(cost, "overrunMax"));
                draft.setAcceptedOverrunRate(number(cost, "overrunRateMax"));
                draft.setBudgetDecisionStatus(BudgetDecisionStatus.OVERRUN_ACCEPTED);
                draft.setStage("FINALIZABLE");
                result.put("status", "OVERRUN_ACCEPTED");
                result.put("instruction", "用户已接受该方案超预算，保留当前方案并在最终版注明预算、预计费用和已接受超出额。");
            }
            case "REVISE_TO_BUDGET" -> {
                draft.setBudgetDecisionStatus(BudgetDecisionStatus.REVISION_REQUIRED);
                selected.setCostStatus("STALE");
                draft.setCostStatus("STALE");
                String preference = text(args, "adjustment_preferences");
                if (preference.isBlank()) {
                    draft.setStage("AWAITING_ADJUSTMENT_PREFERENCE");
                    result.put("status", "AWAITING_ADJUSTMENT_PREFERENCE");
                    result.put("instruction", "只询问用户最希望保留住宿品质、团建活动、餐饮标准还是交通便利性，不自动修改。");
                } else {
                    draft.setStage("OPTION_REVISION_REQUIRED");
                    result.put("status", "OPTION_REVISION_REQUIRED");
                    result.put("instruction", "按用户调整偏好只修改所选方案，重新查询受影响价格并再次核算。");
                }
            }
            case "UPDATE_BUDGET_LIMIT" -> {
                if (args.has("new_budget_total") && args.get("new_budget_total").isNumber()) {
                    draft.setBudgetTotal(args.get("new_budget_total").asDouble());
                    draft.setBudgetPerPerson(null);
                } else if (args.has("new_budget_per_person")
                        && args.get("new_budget_per_person").isNumber()) {
                    draft.setBudgetPerPerson(args.get("new_budget_per_person").asDouble());
                    draft.setBudgetTotal(null);
                } else {
                    return saveError(userId, draft, "更新预算上限时必须提供新的总预算或人均预算。");
                }
                draft.setBudgetDecisionStatus(BudgetDecisionStatus.LIMIT_UPDATED);
                draft.setStage("OPTIONS_READY_FOR_COSTING");
                result.put("status", "LIMIT_UPDATED");
                result.put("next_tool", "budget_calculator");
                result.put("instruction", "方案项目未变，只需用新预算上限重新调用 budget_calculator 完成比较。");
            }
            case "SHOW_ADJUSTMENT_OPTIONS" -> {
                draft.setStage("AWAITING_ADJUSTMENT_SELECTION");
                result.put("status", "AWAITING_ADJUSTMENT_SELECTION");
                result.put("instruction", "根据当前成本明细列出可调整项和预计影响，等待用户选择；不要立即修改或调用外部工具。");
            }
            default -> {
                return saveError(userId, draft, "无法识别预算决定，请确认接受超预算、调整到预算内、更新预算上限或查看调整项。");
            }
        }
        addOutputContract(result);
        refreshState(result, draft);
        stateStore.save(userId, draft);
        return result;
    }

    private ObjectNode handleRevision(String userId, TeamTripPlanDraft draft, String feedback) {
        draft.setLastFeedback(feedback);
        draft.setVersion(Math.max(1, draft.getVersion()) + 1);
        ObjectNode result = baseResult(draft);
        result.put("status", "REVISING");
        result.put("feedback", feedback);
        String normalized = feedback == null ? "" : feedback;

        if (feedback == null || feedback.isBlank()
                || containsAny(normalized, "不满意", "都不好", "换一个")) {
            draft.setStage("AWAITING_REVISION_REASON");
            result.put("next_tool", "none");
            result.put("instruction", "只询问主要不满意预算、目的地、住宿、行程强度、活动内容还是方案差异，不立即生成相似方案。");
        } else if (containsAny(normalized, "贵", "超预算", "便宜")) {
            draft.setBudgetDecisionStatus(BudgetDecisionStatus.REVISION_REQUIRED);
            draft.setStage("AWAITING_ADJUSTMENT_PREFERENCE");
            result.put("next_tool", "none");
            result.put("instruction", "用户已表示不接受当前费用。先确认最希望保留的项目，不自动降低标准或删除活动。");
        } else if (containsAny(normalized, "换地方", "目的地", "不想去")) {
            draft.setDestination(null);
            invalidateAllOptions(draft);
            draft.setStage("READY_FOR_MAP");
            result.put("next_tool", "map_search_place");
            result.put("instruction", "保留人数、日期、时长和预算，重新搜索不同目的地；地图不足后再调用 web_search。");
        } else if (containsAny(normalized, "天气", "下雨", "雨天")) {
            draft.setStage("READY_FOR_WEATHER");
            result.put("next_tool", "weather_query");
            result.put("instruction", "重新核实天气，只调整受影响活动；方案变化后重新核算对应成本。");
        } else {
            TeamTripPlanOption selected = findOption(draft, draft.getSelectedOptionId());
            if (selected != null) {
                selected.setVersion(selected.getVersion() + 1);
                selected.setCostStatus("STALE");
                selected.setCostResult(null);
            }
            draft.setCostStatus("STALE");
            draft.setStage("OPTION_REVISION_REQUIRED");
            result.put("next_tool", "none");
            result.put("instruction", "保留未修改的硬性条件，只调整受反馈影响的方案内容；价格变化后重新核算。");
        }
        addOutputContract(result);
        refreshState(result, draft);
        stateStore.save(userId, draft);
        return result;
    }

    private void recordCostResult(TeamTripPlanDraft draft, JsonNode result) {
        draft.setLastCostResult(result.deepCopy());
        String status = result.path("status").asText("ERROR").toUpperCase();
        draft.setCostStatus(status);
        if (!"SUCCESS".equals(status) && !"PARTIAL".equals(status)) {
            draft.setStage("COST_ERROR");
            return;
        }
        JsonNode plans = result.path("plans");
        if (plans.isArray()) {
            for (JsonNode planCost : plans) {
                TeamTripPlanOption option = findOption(draft, planCost.path("planId").asText());
                if (option == null) continue;
                option.setCostResult(planCost.deepCopy());
                option.setCostStatus("SUCCESS".equals(planCost.path("costStatus").asText())
                        ? "READY" : "PARTIAL");
            }
        }

        if (draft.getOptions().size() > 1) {
            draft.setOptionDecisionStatus(OptionDecisionStatus.AWAITING_OPTION_SELECTION);
            draft.setStage("AWAITING_OPTION_SELECTION");
        } else if (draft.getOptions().size() == 1) {
            TeamTripPlanOption only = draft.getOptions().get(0);
            draft.setSelectedOptionId(only.getOptionId());
            draft.setOptionDecisionStatus(OptionDecisionStatus.OPTION_SELECTED);
            evaluateSelectedBudget(draft, only);
        } else {
            draft.setStage("COST_READY");
        }
    }

    private String renderMissingInformation(TeamTripPlanDraft draft) {
        Map<String, String> missing = findMissing(draft);
        if (missing.isEmpty()) {
            return "还需要你补充方案所需的信息后才能继续。";
        }
        StringBuilder reply = new StringBuilder("为了生成可核算费用的方案，还需要确认：\n");
        int index = 1;
        for (String question : missing.values()) {
            reply.append(index++).append(". ").append(question).append('\n');
        }
        return reply.toString().trim();
    }

    private String renderOptionSelection(TeamTripPlanDraft draft) {
        StringBuilder reply = new StringBuilder("好的，我根据已经确认的需求整理了 ")
                .append(draft.getOptions().size())
                .append(" 个不同方向的方案：\n");
        int index = 1;
        for (TeamTripPlanOption option : draft.getOptions()) {
            reply.append("\n方案").append(index++).append("：")
                    .append(defaultText(option.getDisplayName(), option.getOptionId()))
                    .append("\n");
            appendIfPresent(reply, "方案定位", option.getPositioning());
            appendIfPresent(reply, "主要亮点", option.getHighlights());
            appendIfPresent(reply, "行程安排", option.getItinerarySummary());
            reply.append("费用参考：\n");
            appendCostSummary(reply, option.getCostResult());
        }

        reply.append("\n以上方案你更喜欢哪一个？请回复方案编号或名称。")
                .append("如果都不满意，也可以告诉我希望组合或修改哪些内容。");
        return reply.toString().trim();
    }

    private String renderBudgetDecision(TeamTripPlanDraft draft) {
        TeamTripPlanOption selected = findOption(draft, draft.getSelectedOptionId());
        if (selected == null) {
            return "请先选择一个候选方案，我再为你比较该方案与预算。";
        }

        StringBuilder reply = new StringBuilder("你选择的是：")
                .append(defaultText(selected.getDisplayName(), selected.getOptionId()))
                .append('\n');
        appendCostSummary(reply, selected.getCostResult());
        reply.append("\n该方案预计超出预算。你可以选择：接受超出、调整到预算内、更新预算上限，"
                + "或者先查看可以调整的项目。");
        return reply.toString();
    }

    private void appendCostSummary(StringBuilder reply, JsonNode cost) {
        if (cost == null || cost.isNull()) {
            reply.append("- **费用情况**：尚未完整核算\n");
            return;
        }
        reply.append("- **预计总费用**：").append(formatMoneyRange(
                number(cost, "estimatedTotalMin"), number(cost, "estimatedTotalMax"))).append('\n');
        reply.append("- **预计人均**：").append(formatMoneyRange(
                number(cost, "perPersonMin"), number(cost, "perPersonMax"))).append('\n');
        Double target = number(cost, "targetBudget");
        if (target != null) {
            reply.append("- **预算上限**：").append(formatMoney(target)).append('\n');
        }
        reply.append("- **预算情况**：").append(budgetComparisonText(cost)).append('\n');
        JsonNode missing = cost.path("missingPriceItems");
        if (missing.isArray() && !missing.isEmpty()) {
            reply.append("- **价格完整度**：仍有 ").append(missing.size()).append(" 项待确认\n");
        } else {
            reply.append("- **价格完整度**：完整\n");
        }
    }

    private static void appendIfPresent(StringBuilder reply, String label, String value) {
        if (!blank(value)) reply.append("- ").append(label).append("：").append(value).append('\n');
    }

    private static String budgetComparisonText(JsonNode cost) {
        if (cost == null || cost.isNull()) return "待确认";
        String status = cost.path("budgetStatus").asText("INDETERMINATE");
        if ("OVER_BUDGET".equals(status) || "POSSIBLY_OVER_BUDGET".equals(status)) {
            Double overrun = number(cost, "overrunMax");
            Double rate = number(cost, "overrunRateMax");
            String prefix = "POSSIBLY_OVER_BUDGET".equals(status) ? "可能超出" : "超出";
            if (overrun == null) return prefix + "预算";
            return prefix + " " + formatMoney(overrun)
                    + (rate != null ? "（" + trimNumber(rate) + "%）" : "");
        }
        return budgetStatusText(status);
    }

    private static String priceCompletenessText(JsonNode cost) {
        if (cost == null || cost.isNull()) return "待核算";
        JsonNode missing = cost.path("missingPriceItems");
        return missing.isArray() && !missing.isEmpty()
                ? "缺 " + missing.size() + " 项"
                : "完整";
    }

    private static String formatMoneyRange(Double min, Double max) {
        if (min == null && max == null) return "待确认";
        if (min == null) min = max;
        if (max == null) max = min;
        if (Math.abs(min - max) < 0.005) return formatMoney(min);
        return formatMoney(min) + "～" + formatMoney(max);
    }

    private static String formatMoney(double value) {
        return "¥" + (Math.rint(value) == value
                ? String.format("%.0f", value)
                : String.format("%.2f", value));
    }

    private static String trimNumber(double value) {
        return Math.rint(value) == value
                ? String.format("%.0f", value)
                : String.format("%.2f", value);
    }

    private static String budgetStatusText(String status) {
        return switch (status) {
            case "WITHIN_BUDGET" -> "预算内";
            case "OVER_BUDGET" -> "超出预算";
            case "POSSIBLY_OVER_BUDGET" -> "可能超出预算";
            case "NO_LIMIT" -> "未设置预算上限";
            default -> "需进一步确认";
        };
    }

    private void evaluateSelectedBudget(TeamTripPlanDraft draft, TeamTripPlanOption option) {
        if (!"READY".equals(option.getCostStatus()) || option.getCostResult() == null) {
            draft.setStage("COST_PARTIAL");
            return;
        }
        String budgetStatus = option.getCostResult().path("budgetStatus").asText("INDETERMINATE");
        if ("WITHIN_BUDGET".equals(budgetStatus)) {
            draft.setBudgetDecisionStatus(BudgetDecisionStatus.WITHIN_BUDGET);
            draft.setStage("FINALIZABLE");
        } else if ("OVER_BUDGET".equals(budgetStatus)
                || "POSSIBLY_OVER_BUDGET".equals(budgetStatus)) {
            if (withinPreapprovedTolerance(draft, option.getCostResult())) {
                draft.setBudgetDecisionStatus(BudgetDecisionStatus.OVERRUN_ACCEPTED);
                draft.setStage("FINALIZABLE");
            } else {
                draft.setBudgetDecisionStatus(BudgetDecisionStatus.AWAITING_USER_DECISION);
                draft.setStage("AWAITING_BUDGET_DECISION");
            }
        } else {
            draft.setStage("COST_PARTIAL");
        }
    }

    private boolean withinPreapprovedTolerance(TeamTripPlanDraft draft, JsonNode cost) {
        boolean hasAmount = positive(draft.getMaxOverrunAmount());
        boolean hasRate = positive(draft.getMaxOverrunRate());
        if (!hasAmount && !hasRate) return false;
        double amount = number(cost, "overrunMax") != null ? number(cost, "overrunMax") : Double.MAX_VALUE;
        double rate = number(cost, "overrunRateMax") != null ? number(cost, "overrunRateMax") : Double.MAX_VALUE;
        return (!hasAmount || amount <= draft.getMaxOverrunAmount())
                && (!hasRate || rate <= draft.getMaxOverrunRate());
    }

    private void addBudgetQuestion(ObjectNode result, TeamTripPlanOption option) {
        JsonNode cost = option.getCostResult();
        ObjectNode question = result.putObject("budget_question");
        question.put("option_id", option.getOptionId());
        question.put("plan_name", option.getDisplayName());
        copyNumber(cost, question, "estimatedTotalMin", "estimated_total_min");
        copyNumber(cost, question, "estimatedTotalMax", "estimated_total_max");
        copyNumber(cost, question, "targetBudget", "target_budget");
        copyNumber(cost, question, "overrunMin", "overrun_min");
        copyNumber(cost, question, "overrunMax", "overrun_max");
        copyNumber(cost, question, "overrunRateMax", "overrun_rate_max");
        question.put("prompt", "该方案预计超出预算。请询问用户是否接受、要求调整到预算内、更新预算上限，或先查看可调整项。");
        result.put("instruction", "只展示成本比较并询问用户决定，不自动降低住宿、替换活动或修改交通。");
    }

    private boolean merge(TeamTripPlanDraft draft, JsonNode args) {
        boolean changed = false;
        if (hasText(args, "departure_city")) {
            changed |= !Objects.equals(draft.getDepartureCity(), text(args, "departure_city"));
            draft.setDepartureCity(text(args, "departure_city"));
        }
        if (args.has("participant_count") && args.get("participant_count").canConvertToInt()) {
            int value = args.get("participant_count").asInt();
            changed |= !Objects.equals(draft.getParticipantCount(), value);
            draft.setParticipantCount(value);
        }
        if (hasText(args, "travel_date")) {
            String travelDate = normalizeYearlessDate(text(args, "travel_date"));
            changed |= !Objects.equals(draft.getTravelDate(), travelDate);
            draft.setTravelDate(travelDate);
        }
        if (hasText(args, "duration")) {
            String duration = text(args, "duration");
            changed |= !Objects.equals(draft.getDuration(), duration);
            draft.setDuration(duration);
            parseDuration(draft, duration);
        }
        if (args.has("days") && args.get("days").canConvertToInt()) {
            int value = args.get("days").asInt();
            changed |= !Objects.equals(draft.getDays(), value);
            draft.setDays(value);
        }
        if (args.has("nights") && args.get("nights").canConvertToInt()) {
            int value = args.get("nights").asInt();
            changed |= !Objects.equals(draft.getNights(), value);
            draft.setNights(value);
        }
        if (args.has("option_count") && args.get("option_count").canConvertToInt()) {
            draft.setOptionCount(args.get("option_count").asInt());
        }
        if (args.has("budget_total") && args.get("budget_total").isNumber()) {
            double value = args.get("budget_total").asDouble();
            changed |= !Objects.equals(draft.getBudgetTotal(), value);
            draft.setBudgetTotal(value);
        }
        if (args.has("budget_per_person") && args.get("budget_per_person").isNumber()) {
            double value = args.get("budget_per_person").asDouble();
            changed |= !Objects.equals(draft.getBudgetPerPerson(), value);
            draft.setBudgetPerPerson(value);
        }
        if (hasText(args, "budget_level")) draft.setBudgetLevel(text(args, "budget_level"));
        if (args.has("max_overrun_amount") && args.get("max_overrun_amount").isNumber())
            draft.setMaxOverrunAmount(args.get("max_overrun_amount").asDouble());
        if (args.has("max_overrun_rate") && args.get("max_overrun_rate").isNumber())
            draft.setMaxOverrunRate(args.get("max_overrun_rate").asDouble());
        if (hasText(args, "destination")) {
            changed |= !Objects.equals(draft.getDestination(), text(args, "destination"));
            draft.setDestination(text(args, "destination"));
        }
        if (hasText(args, "travel_scope")) {
            changed |= !Objects.equals(draft.getTravelScope(), text(args, "travel_scope"));
            draft.setTravelScope(text(args, "travel_scope"));
        }

        for (String field : PREFERENCE_FIELDS) {
            if (hasText(args, field)) {
                String value = text(args, field);
                changed |= !Objects.equals(draft.getPreferences().get(field), value);
                draft.getPreferences().put(field, value);
            }
        }
        JsonNode priorities = args.get("priorities");
        if (priorities != null && priorities.isArray()) {
            List<String> values = new ArrayList<>();
            priorities.forEach(node -> {
                if (!node.asText().isBlank()) values.add(node.asText());
            });
            changed |= !draft.getPriorities().equals(values);
            draft.setPriorities(values);
        }
        draft.setPlanMode(draft.getPriorities().isEmpty() ? "BALANCED_DEFAULT" : "PRIORITY");
        return changed;
    }

    private Map<String, String> findMissing(TeamTripPlanDraft draft) {
        Map<String, String> missing = new LinkedHashMap<>();
        if (blank(draft.getDepartureCity())) missing.put("departure_city", "从哪里出发或在哪里集合？");
        if (draft.getParticipantCount() == null || draft.getParticipantCount() <= 0)
            missing.put("participant_count", "预计多少人参加？");
        if (blank(draft.getTravelDate()))
            missing.put("travel_date", "计划什么时候出发？具体日期或大致时间都可以。");
        if (blank(draft.getDuration()) || draft.getDays() == null || draft.getDays() <= 0)
            missing.put("duration", "计划玩多久？请说明天数，例如1天或2天1晚。");
        if (!positive(draft.getBudgetTotal()) && !positive(draft.getBudgetPerPerson()))
            missing.put("budget", "可接受的预算上限是多少？请提供总预算或人均预算，便于与方案成本比较。");
        if (blank(draft.getDestination()) && blank(draft.getTravelScope()))
            missing.put("destination_or_scope", "有确定目的地吗？没有的话，请告诉我可接受的出行范围。");
        return missing;
    }

    private void invalidateAllOptions(TeamTripPlanDraft draft) {
        draft.getOptions().forEach(option -> {
            option.setCostStatus("STALE");
            option.setCostResult(null);
            option.setPlanStatus("NEEDS_REVIEW");
        });
        draft.setCostStatus("STALE");
        draft.setLastCostResult(null);
        draft.setBudgetDecisionStatus(BudgetDecisionStatus.NOT_REQUIRED);
        draft.setOptionDecisionStatus(draft.getOptions().isEmpty()
                ? OptionDecisionStatus.NO_OPTIONS : OptionDecisionStatus.OPTIONS_GENERATED);
    }

    private ObjectNode baseResult(TeamTripPlanDraft draft) {
        ObjectNode result = objectMapper.createObjectNode();
        refreshState(result, draft);
        return result;
    }

    private void refreshState(ObjectNode result, TeamTripPlanDraft draft) {
        result.put("plan_mode", draft.getPlanMode());
        result.put("version", draft.getVersion());
        result.put("stage", draft.getStage());
        result.set("collected_information", objectMapper.valueToTree(draft));
    }

    private void addOutputContract(ObjectNode result) {
        result.putArray("output_contract")
                .add("保持加入预算功能前的自然旅游方案展示结构，不使用大型候选预算对比表或十段式固定报告")
                .add("先自然介绍方案并明确列出方案亮点，再按天展示行程、活动、交通、住宿、餐饮、美食推荐和注意事项")
                .add("美食推荐应包含当地特色菜、适合团队的餐厅类型或特色用餐体验，不能只写泛化的早午晚餐安排")
                .add("预算只作为对应方案末尾的补充，引用 budget_calculator 的总费用、人均、预算差额和待确认价格")
                .add("全部方案展示后再询问用户选择，不得替用户自动决定");
    }

    private ObjectNode saveError(String userId, TeamTripPlanDraft draft, String message) {
        ObjectNode result = baseResult(draft);
        result.put("status", "INVALID_ARGUMENT");
        result.put("error", message);
        stateStore.save(userId, draft);
        return result;
    }

    private TeamTripPlanOption findOption(TeamTripPlanDraft draft, String optionId) {
        if (blank(optionId)) return null;
        return draft.getOptions().stream()
                .filter(option -> optionId.equals(option.getOptionId()))
                .findFirst().orElse(null);
    }

    private void parseDuration(TeamTripPlanDraft draft, String duration) {
        Matcher days = DAYS_PATTERN.matcher(duration);
        if (days.find()) draft.setDays(Integer.parseInt(days.group(1)));
        Matcher nights = NIGHTS_PATTERN.matcher(duration);
        if (nights.find()) draft.setNights(Integer.parseInt(nights.group(1)));
        else if (draft.getDays() != null) draft.setNights(Math.max(draft.getDays() - 1, 0));
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("status", "ERROR");
            error.put("error", "工具结果无法解析");
            return error;
        }
    }

    private boolean needsDateNormalization(String value) {
        if (blank(value)) return false;
        return containsAny(value, "今天", "明天", "后天", "本周", "下周", "上周", "周末")
                && !value.matches(".*\\d{4}[-年]\\d{1,2}[-月]\\d{1,2}.*");
    }

    /**
     * “8月5日”这类无年份日期直接补成下一次到来的日期，避免模型先猜错年份再重复查询。
     */
    private static String normalizeYearlessDate(String value) {
        if (blank(value) || value.matches(".*\\d{4}[-年].*")) return value;
        Matcher matcher = MONTH_DAY_PATTERN.matcher(value);
        if (!matcher.find()) return value;

        int month = Integer.parseInt(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        try {
            LocalDate resolved = LocalDate.of(today.getYear(), month, day);
            if (resolved.isBefore(today)) resolved = resolved.plusYears(1);
            return resolved.toString();
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    private static boolean isInsufficient(String status) {
        return "EMPTY".equals(status) || "PARTIAL".equals(status)
                || "ERROR".equals(status) || "UNAVAILABLE".equals(status);
    }

    /**
     * 节假日与地图查询互不依赖，可在同一轮并行执行。
     * 无论结果到达顺序如何，都要等两类结果均返回后才进入天气阶段。
     */
    private static void advanceInitialResearch(TeamTripPlanDraft draft) {
        boolean holidayDone = !"NOT_CALLED".equals(draft.getHolidayStatus());
        boolean mapDone = !"NOT_CALLED".equals(draft.getMapStatus());
        if (!holidayDone || !mapDone) {
            draft.setStage("READY_FOR_CONTEXT");
            return;
        }
        draft.setStage(isInsufficient(draft.getMapStatus()) ? "MAP_INSUFFICIENT" : "MAP_READY");
    }

    private static boolean isWaitingForUser(TeamTripPlanDraft draft) {
        return Set.of("AWAITING_OPTION_SELECTION", "AWAITING_BUDGET_DECISION",
                        "AWAITING_ADJUSTMENT_PREFERENCE", "AWAITING_ADJUSTMENT_SELECTION",
                        "AWAITING_REVISION_REASON")
                .contains(draft.getStage());
    }

    private static boolean containsAny(String value, String... words) {
        if (value == null) return false;
        for (String word : words) if (value.contains(word)) return true;
        return false;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean positive(Double value) {
        return value != null && value > 0;
    }

    private static boolean hasText(JsonNode node, String field) {
        return !text(node, field).isBlank();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node != null ? node.get(field) : null;
        return value == null || value.isNull() ? "" : value.asText().trim();
    }

    private static String defaultText(String value, String fallback) {
        return blank(value) ? fallback : value;
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node != null ? node.get(field) : null;
        return value != null && value.isNumber() ? value.asDouble() : null;
    }

    private static void copyNumber(JsonNode source, ObjectNode target, String sourceField, String targetField) {
        JsonNode value = source != null ? source.get(sourceField) : null;
        if (value != null && value.isNumber()) target.set(targetField, value);
    }
}
