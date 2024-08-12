package com.axonivy.utils.bpmnstatistic.enums;

public enum TimeIntervalUnit {
  WEEK("Week"), MONTH("Month"), YEAR("Year");

  private String label;

  private TimeIntervalUnit(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

}
