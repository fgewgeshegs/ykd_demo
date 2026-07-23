package com.youkeda.exercise.claw.teamtrip;

import com.fasterxml.jackson.databind.JsonNode;

/** 团建/出游流程中的一个候选方案。 */
public class TeamTripPlanOption {

    private String optionId;
    private String displayName;
    private int version = 1;
    private String positioning;
    private String highlights;
    private String itinerarySummary;
    private String planStatus = "CANDIDATE";
    private String costStatus = "NOT_CALCULATED";
    private JsonNode costResult;

    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getPositioning() { return positioning; }
    public void setPositioning(String positioning) { this.positioning = positioning; }
    public String getHighlights() { return highlights; }
    public void setHighlights(String highlights) { this.highlights = highlights; }
    public String getItinerarySummary() { return itinerarySummary; }
    public void setItinerarySummary(String itinerarySummary) { this.itinerarySummary = itinerarySummary; }
    public String getPlanStatus() { return planStatus; }
    public void setPlanStatus(String planStatus) { this.planStatus = planStatus; }
    public String getCostStatus() { return costStatus; }
    public void setCostStatus(String costStatus) { this.costStatus = costStatus; }
    public JsonNode getCostResult() { return costResult; }
    public void setCostResult(JsonNode costResult) { this.costResult = costResult; }
}
