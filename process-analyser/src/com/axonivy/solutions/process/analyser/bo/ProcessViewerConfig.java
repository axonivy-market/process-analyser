package com.axonivy.solutions.process.analyser.bo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessViewerConfig {
  private List<String> frquencyColor;
  private String frquencyTextColor;
  private List<String> durationColor;
  private String durationTextColor;
  private String widgetSelectedModule;
  private String widgetSelectedProcessName;
  private String widgetSelectedKpi;
  private Boolean widgetMergedProcessStart;
  private String widgetIncludeRunningCase;
  private Boolean widgetHeatMapMode;

  private static final ObjectMapper MAPPER = new ObjectMapper().setSerializationInclusion(Include.NON_EMPTY)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public String toJson() {
    try {
      return MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize A", e);
    }
  }

  public static ProcessViewerConfig fromJson(String json) {
    if (json == null || json.trim().isEmpty())
      return new ProcessViewerConfig();
    try {
      return MAPPER.readValue(json, ProcessViewerConfig.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid JSON for A", e);
    }
  }

  public List<String> getFrquencyColor() {
    return frquencyColor;
  }

  public void setFrquencyColor(List<String> frquencyColor) {
    this.frquencyColor = frquencyColor;
  }

  public String getFrquencyTextColor() {
    return frquencyTextColor;
  }

  public void setFrquencyTextColor(String frquencyTextColor) {
    this.frquencyTextColor = frquencyTextColor;
  }

  public List<String> getDurationColor() {
    return durationColor;
  }

  public void setDurationColor(List<String> durationColor) {
    this.durationColor = durationColor;
  }

  public String getDurationTextColor() {
    return durationTextColor;
  }

  public void setDurationTextColor(String durationTextColor) {
    this.durationTextColor = durationTextColor;
  }

  public String getWidgetSelectedModule() {
    return widgetSelectedModule;
  }

  public void setWidgetSelectedModule(String widgetSelectedModule) {
    this.widgetSelectedModule = widgetSelectedModule;
  }

  public String getWidgetSelectedProcessName() {
    return widgetSelectedProcessName;
  }

  public void setWidgetSelectedProcessName(String widgetSelectedProcessName) {
    this.widgetSelectedProcessName = widgetSelectedProcessName;
  }

  public String getWidgetSelectedKpi() {
    return widgetSelectedKpi;
  }

  public void setWidgetSelectedKpi(String widgetSelectedKpi) {
    this.widgetSelectedKpi = widgetSelectedKpi;
  }

  public Boolean getWidgetMergedProcessStart() {
    return widgetMergedProcessStart;
  }

  public void setWidgetMergedProcessStart(Boolean widgetMergedProcessStart) {
    this.widgetMergedProcessStart = widgetMergedProcessStart;
  }

  public String getWidgetIncludeRunningCase() {
    return widgetIncludeRunningCase;
  }

  public void setWidgetIncludeRunningCase(String widgetIncludeRunningCase) {
    this.widgetIncludeRunningCase = widgetIncludeRunningCase;
  }

  public Boolean getWidgetHeatMapMode() {
    return widgetHeatMapMode;
  }

  public void setWidgetHeatMapMode(Boolean widgetHeatMapMode) {
    this.widgetHeatMapMode = widgetHeatMapMode;
  }
}
