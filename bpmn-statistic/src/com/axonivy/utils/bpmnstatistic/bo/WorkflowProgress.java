package com.axonivy.utils.bpmnstatistic.bo;

import java.io.Serializable;
import java.util.Date;

import com.axonivy.utils.bpmnstatistic.enums.NodeType;

public class WorkflowProgress implements Serializable {
  private static final long serialVersionUID = -628946458018725877L;
  private String processRawPid;
  private String elementId;
  private String originElementId;
  private String targetElementId;
  private Date startTimeStamp;
  private Date endTimeStamp;
  private Long duration;
  private Long caseId;
  private NodeType type;

  public WorkflowProgress(String processRawPid, String rowId, String originElementId, String targetElementId,
      Long caseId, NodeType type) {
    this.processRawPid = processRawPid;
    this.elementId = rowId;
    this.originElementId = originElementId;
    this.targetElementId = targetElementId;
    this.caseId = caseId;
    this.startTimeStamp = new Date();
    this.type = type;
  };

  public WorkflowProgress(String processRawPid, String elementId, Long caseId, NodeType type) {
    this.processRawPid = processRawPid;
    this.elementId = elementId;
    this.caseId = caseId;
    this.startTimeStamp = new Date();
    this.type = type;
  };

  public WorkflowProgress() {
  }

  public Long getCaseId() {
    return caseId;
  }

  public void setCaseId(Long caseId) {
    this.caseId = caseId;
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

  public String getElementId() {
    return elementId;
  }

  public void setElementId(String elementId) {
    this.elementId = elementId;
  }

  public NodeType getType() {
    return type;
  }

  public void setType(NodeType type) {
    this.type = type;
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
    return "WorkflowProgress [processRawPid=" + processRawPid + ", type=" + type + ", elementId=" + elementId + ", originElementId="
        + originElementId + ", targetElementId=" + targetElementId + ", startTimeStamp=" + startTimeStamp
        + ", endTimeStamp=" + endTimeStamp + ", duration=" + duration + ", caseId=" + caseId + "]";
  }
}
