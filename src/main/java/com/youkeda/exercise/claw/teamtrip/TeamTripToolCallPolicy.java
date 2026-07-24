package com.youkeda.exercise.claw.teamtrip;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** 在完整团建方案流程中约束工具调用顺序。 */
@Component
public class TeamTripToolCallPolicy {

    private final TeamTripPlanStateStore stateStore;

    public TeamTripToolCallPolicy(TeamTripPlanStateStore stateStore) {
        this.stateStore = stateStore;
    }

    /** 当前工具结果已经进入必须等待用户输入的阶段，下一轮应只生成文字回复。 */
    public boolean shouldReplyWithoutTools(String userId) {
        TeamTripPlanDraft draft = stateStore.get(userId);
        if (draft == null) return false;
        return List.of("NEED_MORE_INFORMATION", "AWAITING_REVISION_REASON",
                        "AWAITING_OPTION_SELECTION", "AWAITING_BUDGET_DECISION",
                        "AWAITING_ADJUSTMENT_PREFERENCE", "AWAITING_ADJUSTMENT_SELECTION")
                .contains(draft.getStage());
    }

    /**
     * 始终可用的工具：即使用户处于等待阶段也允许调用，
     * 保证用户随时可以要求生成图片、文件或语音。
     */
    private static final Set<String> ALWAYS_AVAILABLE_TOOLS =
            Set.of("file_generate", "image_generate", "text_to_speech",
                    "plan_proposal", "place_image_search");

    /**
     * @return null 表示允许；非空字符串表示阻止原因
     */
    public String validate(String userId, String toolName, List<String> currentBatchTools) {
        TeamTripPlanDraft draft = stateStore.get(userId);
        if (draft == null || "team_trip_plan".equals(toolName)) return null;

        String stage = draft.getStage();
        if (shouldReplyWithoutTools(userId)
                && !ALWAYS_AVAILABLE_TOOLS.contains(toolName)) {
            return "当前流程正在等待用户补充、选择或确认，暂不允许调用外部工具。";
        }
        if (("READY_FOR_DATE".equals(stage) || "READY_FOR_DATE_CONTEXT".equals(stage))
                && !"time_query".equals(toolName) && !toolName.startsWith("map_")) {
            return "必须先使用 time_query 把相对日期换算为明确日期；地图查询可与日期换算并行。";
        }
        if (("READY_FOR_CONTEXT".equals(stage) || "READY_FOR_HOLIDAY".equals(stage))
                && !"holiday_check".equals(toolName) && !toolName.startsWith("map_")) {
            return "日期已明确，必须先并行完成 holiday_check 和地图查询，再进入天气与交通阶段。";
        }
        if ("READY_FOR_MAP".equals(stage) && !toolName.startsWith("map_")) {
            return "完整方案必须先调用地图工具确认地点；读取地图结果后才能调用其他外部工具。";
        }
        if ("MAP_INSUFFICIENT".equals(stage) && !"web_search".equals(toolName)) {
            return "地图服务本轮已返回信息不足或不可用，请停止重复地图查询并改用一次聚焦的 web_search。";
        }
        if ("WEATHER_INSUFFICIENT".equals(stage) && !"web_search".equals(toolName)) {
            return "天气工具信息不足或不可用，请用一次聚焦的 web_search 补充后再继续。";
        }
        if (("WEATHER_READY".equals(stage) || "READY_FOR_TRANSPORT".equals(stage))
                && !"transport_recommend".equals(toolName)) {
            return "天气评估已完成，必须先使用 transport_recommend 比较团建交通方式和费用。";
        }
        if ("web_search".equals(toolName)) {
            boolean sameBatchHasSpecializedTool = currentBatchTools.stream()
                    .anyMatch(name -> name.startsWith("map_") || "weather_query".equals(name)
                            || "time_query".equals(name) || "holiday_check".equals(name)
                            || "transport_recommend".equals(name)
                            || "budget_calculator".equals(name));
            if (sameBatchHasSpecializedTool) {
                return "web_search 不能与专业查询或成本核算工具在同一轮调用。请先读取前一工具结果。";
            }
        }
        if ("budget_calculator".equals(toolName)) {
            boolean sameBatchHasEvidenceTool = currentBatchTools.stream()
                    .anyMatch(name -> name.startsWith("map_") || "weather_query".equals(name)
                            || "transport_recommend".equals(name) || "web_search".equals(name));
            if (sameBatchHasEvidenceTool) {
                return "成本核算必须在读取地点、天气和价格查询结果后单独调用，不能与这些工具同轮执行。";
            }
        }
        if ("weather_query".equals(toolName)
                && (draft.getDestination() == null || draft.getDestination().isBlank())) {
            return "目的地尚未确定，不能查询具体天气。请先通过地图确定目的地。";
        }
        return null;
    }
}
