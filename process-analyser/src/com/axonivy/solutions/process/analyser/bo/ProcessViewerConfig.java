package com.axonivy.solutions.process.analyser.bo;

public class ProcessViewerConfig {
  private String frequencyColor;
  private String frequencyTextColor;
  private String durationColor;
  private String durationTextColor;
  private Boolean isCustomColorMode;
  private String widgetSelectedModule;
  private String widgetSelectedProcessAnalyzer;
  private String widgetSelectedKpi;
  private Boolean widgetMergedProcessStart;
  private Boolean widgetIncludeRunningCase;
  private Boolean widgetHeatMapMode;

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

  public Boolean getWidgetIncludeRunningCase() {
    return widgetIncludeRunningCase;
  }

  public void setWidgetIncludeRunningCase(Boolean widgetIncludeRunningCase) {
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

  public String getDurationColor() {
    return durationColor;
  }

  public void setDurationColor(String durationColor) {
    this.durationColor = durationColor;
  }

  public String getFrequencyColor() {
    return frequencyColor;
  }

  public void setFrequencyColor(String frequencyColor) {
    this.frequencyColor = frequencyColor;
  }

  public String getFrequencyTextColor() {
    return frequencyTextColor;
  }

  public void setFrequencyTextColor(String frequencyTextColor) {
    this.frequencyTextColor = frequencyTextColor;
  }

  public Boolean getIsCustomColorMode() {
    return isCustomColorMode;
  }

  public void setIsCustomColorMode(Boolean isCustomColorMode) {
    this.isCustomColorMode = isCustomColorMode;
  }
}
