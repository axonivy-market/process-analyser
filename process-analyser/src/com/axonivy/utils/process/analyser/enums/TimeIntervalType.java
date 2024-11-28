package com.axonivy.utils.process.analyser.enums;

import java.util.List;

public enum TimeIntervalType implements HasCmsName {

  YESTERDAY, TODAY, CURRENT, WITHIN_THE_LAST, WITHIN_THE_NEXT, BETWEEN;

  public static final List<TimeIntervalType> POINT_SELECTIONS = List.of(YESTERDAY, TODAY);
  public static final List<TimeIntervalType> RANGE_SELECTIONS = List.of(BETWEEN);
  public static final List<TimeIntervalType> WITH_IN_SELECTIONS = List.of(WITHIN_THE_LAST, WITHIN_THE_NEXT);

}
