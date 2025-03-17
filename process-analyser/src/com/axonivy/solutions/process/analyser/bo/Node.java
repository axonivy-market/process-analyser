package com.axonivy.solutions.process.analyser.bo;

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
  private int frequency;
  @JsonIgnore
  private boolean isTask;

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

  public boolean isTask() {
    return isTask;
  }

  public void setTask(boolean isTask) {
    this.isTask = isTask;
  }
}
