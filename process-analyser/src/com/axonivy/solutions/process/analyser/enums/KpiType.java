package com.axonivy.solutions.process.analyser.enums;

import java.util.Arrays;
import java.util.List;

public enum KpiType implements HasCmsName {
  FREQUENCY(null),
  DURATION(null),
  DURATION_IDLE(DURATION),
  DURATION_WORKING(DURATION),
  DURATION_OVERALL(DURATION),
  DURATION_IDLE_DAY(DURATION_IDLE),
  DURATION_IDLE_HOUR(DURATION_IDLE),
  DURATION_IDLE_MINUTE(DURATION_IDLE),
  DURATION_IDLE_SECOND(DURATION_IDLE),
  DURATION_WORKING_DAY(DURATION_WORKING),
  DURATION_WORKING_HOUR(DURATION_WORKING),
  DURATION_WORKING_MINUTE(DURATION_WORKING),
  DURATION_WORKING_SECOND(DURATION_WORKING),
  DURATION_OVERALL_DAY(DURATION_OVERALL),
  DURATION_OVERALL_HOUR(DURATION_OVERALL),
  DURATION_OVERALL_MINUTE(DURATION_OVERALL),
  DURATION_OVERALL_SECOND(DURATION_OVERALL);

  private final KpiType parent;

  KpiType(KpiType parent) {
    this.parent = parent;
  }

  public KpiType getParent() {
    return parent;
  }

  public boolean isDescendantOf(KpiType ancestorType) {
    if (ancestorType == null) {
      return false;
    }
    return this == ancestorType || (parent != null && parent.isDescendantOf(ancestorType));
  }

  public static List<KpiType> getTopLevelOptions() {
    return Arrays.asList(FREQUENCY, DURATION);
  }

  public List<KpiType> getSubOptions() {
    return Arrays.stream(KpiType.values()).filter(kpi -> kpi.getParent() == this).toList();
  }
}
