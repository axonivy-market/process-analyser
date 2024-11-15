package com.axonivy.utils.bpmnstatistic.constants;

public class ProcessAnalyticsConstants {
  public static final String REMOVE_DEFAULT_HIGHLIGHT_JS_FUNCTION = "santizeDiagram();";
  public static final String UPDATE_FREQUENCY_COUNT_FOR_TASK_FUNCTION = "addElementFrequency('%s', '%s', '%s', '%s');";
  public static final String FREQUENCY_BACKGROUND_COLOR_LEVEL_VARIABLE_PATTERN = "frequencyBackgroundColorLevel%s";
  public static final int DEFAULT_BACKGROUND_COLOR_LEVEL = 1;
  public static final int HIGHEST_LEVEL_OF_BACKGROUND_COLOR = 6;
  public static final String ADDITIONAL_INFORMATION_FORMAT = "%s instances";
  public static final String UPDATE_ADDITIONAL_INFORMATION_FUNCTION = "renderAdditionalInformation('%s')";
  public static final int DEFAULT_INITIAL_STATISTIC_NUMBER = 0;
  public static final String HYPHEN_SIGN = "-";
  public static final String QUOTED_CONTENT_PATTERN = "\"([^\"]*)\"";
  public static final String BPMN_STATISTIC_PMV_NAME = "bpmn-statistic";
  public static final String PORTAL_PMV_SUFFIX = "portal";
  public static final String FROM = "from";
  public static final String TO = "to";
  public static final String COMMA_CONNECT_PATTERN = ", ";
  public static final String HYPHEN_CONNECT_PATTERN = " - ";
  public static final String CURRENT_TIME_PATTERN = "%s - %s %s";
  public static final String WHOLE_DAY_PATTERN = "%s - %s, %s %s, %s";
  public static final String BPMN_STATISTIC_SOURCE_URL_PATTERN = "/%s/faces/view/bpmn-statistic/process-miner.xhtml?server=%s&app=%s&pmv=%s&file=/processes/%s";
  public static final String PROCESS_MINING_CMS_PATH = "process-mining";
  public static final String DATA_CMS_PATH = "data";
  public static final String JSON_EXTENSION = "json";
  public static final String EN_CMS_LOCALE = "en";
  public static final String SLASH = "/";
  public static final String MODULE_PATH = "%s/%s/";
  public static final String ANALYSIS_EXCEL_FILE_PATTERN = "Analysis_Of_%s";
  public static final String LIKE_TEXT_SEARCH = "%%%s%%";
}
