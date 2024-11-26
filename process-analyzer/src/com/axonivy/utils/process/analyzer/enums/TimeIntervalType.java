package com.axonivy.utils.process.analyzer.enums;

import java.util.List;

public enum TimeIntervalType implements HasCmsName {

  YESTERDAY, TODAY, BETWEEN, CUSTOM;

  public static final List<TimeIntervalType> POINT_SELECTIONS = List.of(YESTERDAY, TODAY);
}
