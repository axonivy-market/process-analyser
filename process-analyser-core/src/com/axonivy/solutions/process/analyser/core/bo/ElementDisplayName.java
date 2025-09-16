package com.axonivy.solutions.process.analyser.core.bo;

import com.axonivy.solutions.process.analyser.core.enums.ElementType;

public class ElementDisplayName {
  private String pid;
  private String displayName;
  private ElementType elementType;

  public ElementDisplayName() { }

  public ElementDisplayName(String pid, String displayName) {
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

  public ElementType getElementType() {
    return elementType;
  }

  public void setElementType(ElementType elementType) {
    this.elementType = elementType;
  }
}
