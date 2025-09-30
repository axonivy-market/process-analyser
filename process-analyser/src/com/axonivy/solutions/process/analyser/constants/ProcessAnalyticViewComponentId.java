package com.axonivy.solutions.process.analyser.constants;

import java.util.List;

public class ProcessAnalyticViewComponentId {
  private static final String FORM = "process-analytics-form";
  private static final String ID_SECTION_SEPARATOR = ":";
  public static final String PROCESS_SELECTION_GROUP = FORM + ID_SECTION_SEPARATOR + "process-selection-group";
  public static final String ARROW_STATISTIC = FORM + ID_SECTION_SEPARATOR + "arrow-statistics";
  public static final String HIDDEN_IMAGE = FORM + ID_SECTION_SEPARATOR + "hidden-image";
  public static final String PROCESS_ANALYTIC_VIEWER_PANEL = FORM + ID_SECTION_SEPARATOR
      + "process-analytic-viewer-panel" + ID_SECTION_SEPARATOR + "viewer-group";
  public static final String SHOW_STATISTIC_BTN = FORM + ID_SECTION_SEPARATOR + "show-statistic-btn";
  public static final String CUSTOM_FILTER_PANEL = FORM + ID_SECTION_SEPARATOR + "custom-filter-panel-group";
  public static final String CUSTOM_FILTER_GROUP = CUSTOM_FILTER_PANEL + ID_SECTION_SEPARATOR + "custom-filter-group";
  public static final String CUSTOM_FILTER_OPTIONS_GROUP = CUSTOM_FILTER_PANEL + ID_SECTION_SEPARATOR
      + "filter-options-group";
  public static final String ACTION_BTN_GROUP = FORM + ID_SECTION_SEPARATOR + "action-btn-group";

  public static List<String> getDiagramAndStatisticComponentIds() {
    return List.of(ARROW_STATISTIC, HIDDEN_IMAGE, PROCESS_ANALYTIC_VIEWER_PANEL, SHOW_STATISTIC_BTN, ACTION_BTN_GROUP);
  }
}
