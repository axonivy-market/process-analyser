package com.axonivy.solutions.process.analyser.bo;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Node {
  private NodeType type;
  private String id;
  @JsonIgnore
  private String label;
  private String labelValue;
  private float relativeValue;
  @JsonIgnore
  private float medianDuration;
  @JsonIgnore
  private String duration;
  @JsonIgnore
  private int frequency;
  @JsonIgnore
  private List<String> outGoingPathIds = new ArrayList<>();
  @JsonIgnore
  private List<String> inCommingPathIds = new ArrayList<>();
  @JsonIgnore
  private String targetNodeId;
  @JsonIgnore
  private boolean isTaskSwitchGateway;
  @JsonIgnore
  private String requestPath;
  

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public Node() {}

  public NodeType getType() {
    return type;
  }

  public void setType(NodeType type) {
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getLabelValue() {
    return labelValue;
  }

  public void setLabelValue(String labelValue) {
    this.labelValue = labelValue;
  }

  public float getRelativeValue() {
    return relativeValue;
  }

  public void setRelativeValue(float relativeValue) {
    this.relativeValue = relativeValue;
  }

  public float getMedianDuration() {
    return medianDuration;
  }

  public void setMedianDuration(float medianDuration) {
    this.medianDuration = medianDuration;
  }

  public int getFrequency() {
    return frequency;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }

  public List<String> getOutGoingPathIds() {
    return outGoingPathIds;
  }

  public void setOutGoingPathIds(List<String> outGoingPathIds) {
    this.outGoingPathIds = outGoingPathIds;
  }

  public List<String> getInCommingPathIds() {
    return inCommingPathIds;
  }

  public void setInCommingPathIds(List<String> inCommingPathIds) {
    this.inCommingPathIds = inCommingPathIds;
  }

  public String getTargetNodeId() {
    return targetNodeId;
  }

  public void setTargetNodeId(String targetNodeId) {
    this.targetNodeId = targetNodeId;
  }

  public boolean isTaskSwitchGateway() {
    return isTaskSwitchGateway;
  }

  public void setTaskSwitchGateway(boolean isTaskSwitchGateway) {
    this.isTaskSwitchGateway = isTaskSwitchGateway;
  }

  public String getRequestPath() {
    return requestPath;
  }

  public void setRequestPath(String requestPath) {
    this.requestPath = requestPath;
  }
}
