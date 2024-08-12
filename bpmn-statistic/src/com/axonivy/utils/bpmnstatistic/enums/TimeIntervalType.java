package com.axonivy.utils.bpmnstatistic.enums;

import java.util.List;

public enum TimeIntervalType {

  YESTERDAY("Yesterday"), TODAY("Today"), CURRENT("Current"), WITHIN_THE_LAST("Within the last"),
  WITHIN_THE_NEXT("Within the next"), BETWEEN("Between");

  private String label;

  private TimeIntervalType(String label) {
    this.label = label;
  }

  public static final List<TimeIntervalType> POINT_SELECTIONS = List.of(YESTERDAY, TODAY);
  public static final List<TimeIntervalType> RANGE_SELECTIONS = List.of(BETWEEN);
  public static final List<TimeIntervalType> WITH_IN_SELECTIONS = List.of(WITHIN_THE_LAST, WITHIN_THE_NEXT);

  public String getLabel() {
    return label;
  }
}
