package com.axonivy.solutions.process.analyser.core.enums;

public enum ViewerParam {
  FACES("faces"), VIEW("view"), SERVER("server"), APP("app"), PMV("pmv"), FILE("file"), HIGHLIGHT("highlight"),
  SELECT("select"), ZOOM("zoom"), PROCESS_MINER_FILE("process-miner.xhtml");

  private String value;

  private ViewerParam(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

}
