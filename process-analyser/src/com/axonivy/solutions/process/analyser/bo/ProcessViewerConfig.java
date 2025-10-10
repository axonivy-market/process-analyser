package com.axonivy.solutions.process.analyser.bo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessViewerConfig {
  private String frquencyColor;
  private String frquencyTextColor;
  private String durationColor;
  private String durationTextColor;
  private String widgetSelectedModule;
  private String widgetSelectedProcessAnalyzer;
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

  public String getFrquencyTextColor() {
    return frquencyTextColor;
  }

  public void setFrquencyTextColor(String frquencyTextColor) {
    this.frquencyTextColor = frquencyTextColor;
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

  public String getWidgetSelectedProcessAnalyzer() {
    return widgetSelectedProcessAnalyzer;
  }

  public void setWidgetSelectedProcessAnalyzer(String widgetSelectedProcessAnalyzer) {
    this.widgetSelectedProcessAnalyzer = widgetSelectedProcessAnalyzer;
  }

  public String getFrquencyColor() {
    return frquencyColor;
  }

  public void setFrquencyColor(String frquencyColor) {
    this.frquencyColor = frquencyColor;
  }

  public String getDurationColor() {
    return durationColor;
  }

  public void setDurationColor(String durationColor) {
    this.durationColor = durationColor;
  }
}
