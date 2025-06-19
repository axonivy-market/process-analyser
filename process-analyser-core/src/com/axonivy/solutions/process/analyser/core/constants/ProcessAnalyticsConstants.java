package com.axonivy.solutions.process.analyser.core.constants;

import ch.ivyteam.ivy.process.rdm.IProcess;

public class ProcessAnalyticsConstants {
  public static final int DEFAULT_INITIAL_STATISTIC_NUMBER = 0;
  public static final int GRADIENT_COLOR_LEVELS = 10;
  public static final String HYPHEN_SIGN = "-";
  public static final String HYPHEN_REGEX = "\\-";
  public static final String DEFAULT_SECURITY_CONTEXT = "default";
  public static final String PROCESS_ANALYSER_PMV_NAME = "process-analyser";
  public static final String PORTAL_PMV_SUFFIX = "portal";
  public static final String FROM = "from";
  public static final String TO = "to";
  public static final String COMMA = ",";
  public static final String HASHTAG = "#";
  public static final String COMMA_CONNECT_PATTERN = ", ";
  public static final String CURRENT_TIME_PATTERN = "%s - %s %s";
  public static final String WHOLE_DAY_PATTERN = "%s - %s, %s %s, %s";
  public static final String PROCESS_ANALYSER_SOURCE_URL_PATTERN = "%s/faces/view/%s/process-miner.xhtml?server=%s&app=%s&pmv=%s&file=/processes/%s";
  public static final String PROCESS_ANALYSER_CMS_PATH = "process-analyser";
  public static final String DATA_CMS_PATH = "data";
  public static final String JSON_EXTENSION = "json";
  public static final String EN_CMS_LOCALE = "en";
  public static final String SLASH = "/";
  public static final String MODULE_PATH = "%s/%s/";
  public static final String ANALYSIS_EXCEL_FILE_PATTERN = "Analysis_%s_Of_%s";
  public static final String LIKE_TEXT_SEARCH = "%%%s%%";
  public static final String UPDATE_IFRAME_SOURCE_METHOD_CALL = "updateUrlForIframe()";
  public static final String PROCESSFILE_EXTENSION = IProcess.PROCESSFILE_EXTENSION;
  public static final String UNDERSCORE = "_";
  public static final String SPACE_DASH_REGEX = "[\\s-]+";
  public static final String MULTIPLE_UNDERSCORES_REGEX = "_+";
  public static final String NON_DIGIT_REGEX = "[^0-9,]";
  public static final String NON_DIGIT_COMMA_REGEX = "[^\\d,]";
  public static final String HASHTAG = "#";
  public static final String COLOR_SEGMENT_ATTRIBUTE = "segmentIndex";
  public static final String RGB_REGEX_PATTERN = "rgb\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)";
  public static final String RGB_FORMAT = "rgb(%d, %d, %d)";
  public static final String RGB_PREFIX = "rgb";
  public static final String LIGHT_TEXT_COLOR = "#FFFFFF";
  public static final String DARK_TEXT_COLOR = "#000000";
}
