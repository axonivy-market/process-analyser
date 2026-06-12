package com.axonivy.utils.process.analyser.enums;

public enum IvyVariable {
  MAX_QUERY_RESULTS("com.axonivy.utils.process.analyser.maxQueryResults");

  private String variableName;

  private IvyVariable(String variableName) {
    this.variableName = variableName;
  }

  public String getVariableName() {
    return variableName;
  }
}