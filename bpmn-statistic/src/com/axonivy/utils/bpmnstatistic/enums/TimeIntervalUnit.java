package com.axonivy.utils.bpmnstatistic.enums;

import ch.ivyteam.ivy.environment.Ivy;

public enum TimeIntervalUnit {
  WEEK, MONTH, YEAR;

  @Override
  public String toString() {
    return Ivy.cms().co("/Enums/" + getClass().getSimpleName() + "/" + name());
  }
}
