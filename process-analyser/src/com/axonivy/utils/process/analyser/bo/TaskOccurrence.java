package com.axonivy.utils.process.analyser.bo;

public class TaskOccurrence {
  private Long startSwitchEventId;
  private Integer occurrence;

  public TaskOccurrence(Long startSwitchEventId, Integer occurrence) {
    super();
    this.startSwitchEventId = startSwitchEventId;
    this.occurrence = occurrence;
  }

  public Long getStartSwitchEventId() {
    return startSwitchEventId;
  }

  public void setStartSwitchEventId(Long startSwitchEventId) {
    this.startSwitchEventId = startSwitchEventId;
  }

  public Integer getOccurrence() {
    return occurrence;
  }

  public void setOccurrence(Integer occurrence) {
    this.occurrence = occurrence;
  }
}
