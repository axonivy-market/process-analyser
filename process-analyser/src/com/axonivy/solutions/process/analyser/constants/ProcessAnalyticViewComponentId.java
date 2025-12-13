package com.axonivy.solutions.process.analyser.constants;

import java.util.List;

public class ProcessAnalyticViewComponentId {
  private static final String FORM = "process-analytics-form";
  public static final String ID_SECTION_SEPARATOR = ":";
  private static final String STANDARD_FILTER_PANEL_GROUP = FORM + ID_SECTION_SEPARATOR + "standard-filter-panel-group";
  private static final String PROCESS_ANALYTIC_VIEWER_PANEL = FORM + ID_SECTION_SEPARATOR
      + "process-analytic-viewer-panel";
  private static final String WIDGET_STANDRAD_FILTER = PROCESS_ANALYTIC_VIEWER_PANEL + ID_SECTION_SEPARATOR
      + "standard-filter";
  public static final String PROCESS_SELECTION_GROUP = STANDARD_FILTER_PANEL_GROUP + ID_SECTION_SEPARATOR
      + "process-selection-group";

  public static final String WIDGET_PROCESS_SELECTION_GROUP = WIDGET_STANDRAD_FILTER + ID_SECTION_SEPARATOR
      + "process-selection-group";
  private static final String HIDDEN_IMAGE = FORM + ID_SECTION_SEPARATOR + "hidden-image";
  public static final String PROCESS_ANALYTIC_VIEWER_GROUP = PROCESS_ANALYTIC_VIEWER_PANEL + ID_SECTION_SEPARATOR
      + "viewer-group";
  private static final String SHOW_STATISTIC_BTN = STANDARD_FILTER_PANEL_GROUP + ID_SECTION_SEPARATOR
      + "show-statistic-btn";
  private static final String CUSTOM_FILTER_PANEL = FORM + ID_SECTION_SEPARATOR + "custom-filter-panel-group";
  public static final String CUSTOM_FILTER_GROUP = CUSTOM_FILTER_PANEL + ID_SECTION_SEPARATOR + "custom-filter-group";
  public static final String CUSTOM_FILTER_OPTIONS_GROUP = CUSTOM_FILTER_PANEL + ID_SECTION_SEPARATOR
      + "filter-options-group";
  private static final String ACTION_BTN_GROUP = FORM + ID_SECTION_SEPARATOR + "action-btn-group";
  private static final String DATA_STATISTICS = FORM + ID_SECTION_SEPARATOR + "statistic-viewer" + ID_SECTION_SEPARATOR
      + "data-statistics";
  public static final String PMV_GROUP = STANDARD_FILTER_PANEL_GROUP + ID_SECTION_SEPARATOR + "pmv-selection-group";
  public static final String WIDGET_PMV_GROUP = WIDGET_STANDRAD_FILTER + ID_SECTION_SEPARATOR + "pmv-selection-group";
  public static final String ROLE_SELECTION_GROUP = STANDARD_FILTER_PANEL_GROUP + ID_SECTION_SEPARATOR + "role-selection-group";
  public static final String WIDGET_ROLE_SELECTION_GROUP = WIDGET_STANDRAD_FILTER + ID_SECTION_SEPARATOR + "role-selection-group";
  public static List<String> getDiagramAndStatisticComponentIds() {
    return List.of(HIDDEN_IMAGE, PROCESS_ANALYTIC_VIEWER_GROUP, SHOW_STATISTIC_BTN, ACTION_BTN_GROUP, DATA_STATISTICS);
  }
}
