package com.axonivy.utils.bpmnstatistic.bo;

import java.io.Serializable;
import java.util.Date;

public class WorkflowProgress implements Serializable {
  private static final long serialVersionUID = 932880898772989547L;
  private String processRawPid;
  private String arrowId;
  private String originElementId;
  private String targetElementId;
  private Date createdAt;
  private Date startTimeStamp;
  private Date endTimeStamp;
  private Long duration;
  private Long caseId;
  private String condition;
  private boolean isDurationUpdated;
  private boolean isConditionTrue;

  public WorkflowProgress() {
  }

  public Long getCaseId() {
    return caseId;
  }

  public void setCaseId(Long caseId) {
    this.caseId = caseId;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getStartTimeStamp() {
    return startTimeStamp;
  }

  public void setStartTimeStamp(Date startTimeStamp) {
    this.startTimeStamp = startTimeStamp;
  }

  public Date getEndTimeStamp() {
    return endTimeStamp;
  }

  public void setEndTimeStamp(Date endTimeStamp) {
    this.endTimeStamp = endTimeStamp;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public String getProcessRawPid() {
    return processRawPid;
  }

  public void setProcessRawPid(String processRawPid) {
    this.processRawPid = processRawPid;
  }

  public String getArrowId() {
    return arrowId;
  }

  public void setArrowId(String arrowId) {
    this.arrowId = arrowId;
  }

  public String getOriginElementId() {
    return originElementId;
  }

  public void setOriginElementId(String originElementId) {
    this.originElementId = originElementId;
  }

  public String getTargetElementId() {
    return targetElementId;
  }

  public void setTargetElementId(String targetElementId) {
    this.targetElementId = targetElementId;
  }

  @Override
  public String toString() {
    return "WorkflowProgress [processRawPid=" + processRawPid + ", arrowId=" + arrowId + ", originElementId="
        + originElementId + ", targetElementId=" + targetElementId + ", createdAt=" + createdAt + ", startTimeStamp=" + startTimeStamp
        + ", endTimeStamp=" + endTimeStamp + ", duration=" + duration + ", caseId=" + caseId + ", condition="
        + condition + ", isDurationUpdated=" + isDurationUpdated + ", isConditionTrue="
        + isConditionTrue + "]";
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public boolean isConditionTrue() {
    return isConditionTrue;
  }

  public void setConditionTrue(boolean isConditionTrue) {
    this.isConditionTrue = isConditionTrue;
  }

  public boolean isDurationUpdated() {
    return isDurationUpdated;
  }

  public void setDurationUpdated(boolean isDurationUpdated) {
    this.isDurationUpdated = isDurationUpdated;
  }
}
