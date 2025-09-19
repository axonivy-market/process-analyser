package com.axonivy.solutions.process.analyser.core.bo;

import com.axonivy.solutions.process.analyser.core.enums.StartElementType;

public class StartElement {
  private Long taskStartId;
  private String pid;
  private String name;
  private StartElementType type;

  public StartElement() { }

  public StartElement(Long taskStartId, String pid, String name) {
    this.taskStartId = taskStartId;
    this.pid = pid;
    this.name = name;
  }

  public Long getTaskStartId() {
    return taskStartId;
  }

  public void setTaskStartId(Long taskStartId) {
    this.taskStartId = taskStartId;
  }

  public String getPid() {
    return pid;
  }

  public void setPid(String pid) {
    this.pid = pid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public StartElementType getType() {
    return type;
  }

  public void setType(StartElementType type) {
    this.type = type;
  }
}
