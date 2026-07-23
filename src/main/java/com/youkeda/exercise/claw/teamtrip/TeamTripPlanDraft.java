package com.youkeda.exercise.claw.teamtrip;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 团建/出游方案草稿，保存跨轮对话中的结构化需求和流程状态。 */
public class TeamTripPlanDraft {

    private String departureCity;
    private Integer participantCount;
    private String travelDate;
    private String duration;
    private Integer days;
    private Integer nights;
    private Double budgetTotal;
    private Double budgetPerPerson;
    private String budgetLevel;
    private Double maxOverrunAmount;
    private Double maxOverrunRate;
    private Double acceptedOverrunAmount;
    private Double acceptedOverrunRate;
    private String destination;
    private String travelScope;
    private Map<String, String> preferences = new LinkedHashMap<>();
    private List<String> priorities = new ArrayList<>();
    private String planMode = "BALANCED_DEFAULT";
    private String stage = "COLLECTING";
    private int version = 1;
    private String lastFeedback;
    private List<TeamTripPlanOption> options = new ArrayList<>();
    private int optionCount = 3;
    private String selectedOptionId;
    private int optionSetVersion;
    private OptionDecisionStatus optionDecisionStatus = OptionDecisionStatus.NO_OPTIONS;
    private BudgetDecisionStatus budgetDecisionStatus = BudgetDecisionStatus.NOT_REQUIRED;
    private String mapStatus = "NOT_CALLED";
    private String routeStatus = "NOT_CALLED";
    private String weatherStatus = "NOT_CALLED";
    private String webSearchStatus = "NOT_CALLED";
    private String costStatus = "NOT_CALCULATED";
    private JsonNode lastCostResult;

    public String getDepartureCity() { return departureCity; }
    public void setDepartureCity(String departureCity) { this.departureCity = departureCity; }
    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }
    public String getTravelDate() { return travelDate; }
    public void setTravelDate(String travelDate) { this.travelDate = travelDate; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Integer getNights() { return nights; }
    public void setNights(Integer nights) { this.nights = nights; }
    public Double getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(Double budgetTotal) { this.budgetTotal = budgetTotal; }
    public Double getBudgetPerPerson() { return budgetPerPerson; }
    public void setBudgetPerPerson(Double budgetPerPerson) { this.budgetPerPerson = budgetPerPerson; }
    public String getBudgetLevel() { return budgetLevel; }
    public void setBudgetLevel(String budgetLevel) { this.budgetLevel = budgetLevel; }
    public Double getMaxOverrunAmount() { return maxOverrunAmount; }
    public void setMaxOverrunAmount(Double maxOverrunAmount) { this.maxOverrunAmount = maxOverrunAmount; }
    public Double getMaxOverrunRate() { return maxOverrunRate; }
    public void setMaxOverrunRate(Double maxOverrunRate) { this.maxOverrunRate = maxOverrunRate; }
    public Double getAcceptedOverrunAmount() { return acceptedOverrunAmount; }
    public void setAcceptedOverrunAmount(Double acceptedOverrunAmount) {
        this.acceptedOverrunAmount = acceptedOverrunAmount;
    }
    public Double getAcceptedOverrunRate() { return acceptedOverrunRate; }
    public void setAcceptedOverrunRate(Double acceptedOverrunRate) {
        this.acceptedOverrunRate = acceptedOverrunRate;
    }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getTravelScope() { return travelScope; }
    public void setTravelScope(String travelScope) { this.travelScope = travelScope; }
    public Map<String, String> getPreferences() { return preferences; }
    public void setPreferences(Map<String, String> preferences) {
        this.preferences = preferences != null ? preferences : new LinkedHashMap<>();
    }
    public List<String> getPriorities() { return priorities; }
    public void setPriorities(List<String> priorities) {
        this.priorities = priorities != null ? priorities : new ArrayList<>();
    }
    public String getPlanMode() { return planMode; }
    public void setPlanMode(String planMode) { this.planMode = planMode; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getLastFeedback() { return lastFeedback; }
    public void setLastFeedback(String lastFeedback) { this.lastFeedback = lastFeedback; }
    public List<TeamTripPlanOption> getOptions() { return options; }
    public void setOptions(List<TeamTripPlanOption> options) {
        this.options = options != null ? options : new ArrayList<>();
    }
    public int getOptionCount() { return optionCount; }
    public void setOptionCount(int optionCount) { this.optionCount = optionCount; }
    public String getSelectedOptionId() { return selectedOptionId; }
    public void setSelectedOptionId(String selectedOptionId) { this.selectedOptionId = selectedOptionId; }
    public int getOptionSetVersion() { return optionSetVersion; }
    public void setOptionSetVersion(int optionSetVersion) { this.optionSetVersion = optionSetVersion; }
    public OptionDecisionStatus getOptionDecisionStatus() { return optionDecisionStatus; }
    public void setOptionDecisionStatus(OptionDecisionStatus optionDecisionStatus) {
        this.optionDecisionStatus = optionDecisionStatus != null
                ? optionDecisionStatus : OptionDecisionStatus.NO_OPTIONS;
    }
    public BudgetDecisionStatus getBudgetDecisionStatus() { return budgetDecisionStatus; }
    public void setBudgetDecisionStatus(BudgetDecisionStatus budgetDecisionStatus) {
        this.budgetDecisionStatus = budgetDecisionStatus != null
                ? budgetDecisionStatus : BudgetDecisionStatus.NOT_REQUIRED;
    }
    public String getMapStatus() { return mapStatus; }
    public void setMapStatus(String mapStatus) { this.mapStatus = mapStatus; }
    public String getRouteStatus() { return routeStatus; }
    public void setRouteStatus(String routeStatus) { this.routeStatus = routeStatus; }
    public String getWeatherStatus() { return weatherStatus; }
    public void setWeatherStatus(String weatherStatus) { this.weatherStatus = weatherStatus; }
    public String getWebSearchStatus() { return webSearchStatus; }
    public void setWebSearchStatus(String webSearchStatus) { this.webSearchStatus = webSearchStatus; }
    public String getCostStatus() { return costStatus; }
    public void setCostStatus(String costStatus) { this.costStatus = costStatus; }
    public JsonNode getLastCostResult() { return lastCostResult; }
    public void setLastCostResult(JsonNode lastCostResult) { this.lastCostResult = lastCostResult; }
}
