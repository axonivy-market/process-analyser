package com.axonivy.solutions.process.analyser.core.bo;

public class ElementDisplayName {
  private String pid;
  private String displayName;

  public ElementDisplayName() {
    super();
  }

  public ElementDisplayName(String pid, String displayName) {
    super();
    this.pid = pid;
    this.displayName = displayName;
  }

  public String getPid() {
    return pid;
  }

  public void setPid(String pid) {
    this.pid = pid;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
}
