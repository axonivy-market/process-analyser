package com.axonivy.solutions.process.analyser.enums;

import java.util.Arrays;
import java.util.List;

public enum KpiType implements HasCmsName {
  FREQUENCY(null),
  DURATION(null),
  DURATION_DAY(DURATION),
  DURATION_HOUR(DURATION),
  DURATION_MINUTE(DURATION),
  DURATION_SECOND(DURATION);

  private final KpiType parent;

  KpiType(KpiType parent) {
    this.parent = parent;
  }

  public KpiType getParent() {
    return parent;
  }

  public static List<KpiType> getTopLevelOptions() {
    return Arrays.asList(FREQUENCY, DURATION);
  }

  public static List<KpiType> getSubOptions(KpiType parent) {
    return Arrays.stream(KpiType.values())
          .filter(kpi -> kpi.getParent() == parent)
          .toList();
  }
}
