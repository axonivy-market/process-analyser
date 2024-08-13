package com.axonivy.utils.bpmnstatistic.enums;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;

public enum TimeIntervalType {

  YESTERDAY, TODAY, CURRENT, WITHIN_THE_LAST, WITHIN_THE_NEXT, BETWEEN;

  public static final List<TimeIntervalType> POINT_SELECTIONS = List.of(YESTERDAY, TODAY);
  public static final List<TimeIntervalType> RANGE_SELECTIONS = List.of(BETWEEN);
  public static final List<TimeIntervalType> WITH_IN_SELECTIONS = List.of(WITHIN_THE_LAST, WITHIN_THE_NEXT);

  @Override
  public String toString() {
    return Ivy.cms().co("/Enums/" + getClass().getSimpleName() + "/" + name());
  }
}
