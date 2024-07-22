package com.axonivy.utils.bpmnstatistic.enums;

public enum IvyVariable {
  MAX_QUERY_RESULTS("maxQueryResults"), FREQUENCY_NUMBER_COLOR("frequencyNumberColor");

  private String variableName;

  private IvyVariable(String variableName) {
    this.variableName = variableName;
  }

  public String getVariableName() {
    return variableName;
  }
}