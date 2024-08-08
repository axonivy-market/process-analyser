package com.axonivy.utils.bpmnstatistic.enums;

public enum AnalysisType {
  FREQUENCY("frequency"), DURATION("duration");

  private String name;

  private AnalysisType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
