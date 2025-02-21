package com.axonivy.solutions.process.analyser.enums;

import java.util.List;

public enum TimeIntervalType implements HasCmsName {

  YESTERDAY, TODAY, CUSTOM;

  public static final List<TimeIntervalType> POINT_SELECTIONS = List.of(YESTERDAY, TODAY);

}
