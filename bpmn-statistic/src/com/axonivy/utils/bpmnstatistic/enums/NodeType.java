package com.axonivy.utils.bpmnstatistic.enums;

public enum NodeType {
  ELEMENT("element"), ARROW("arrow");

  private String name;

  private NodeType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}