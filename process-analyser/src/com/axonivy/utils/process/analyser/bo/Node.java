package com.axonivy.utils.process.analyser.bo;

import com.axonivy.utils.process.analyser.enums.NodeType;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Node {
  private NodeType type;
  private String id;
  @JsonIgnore
  private String label;
  private int labelValue;
  private double relativeValue;
  @JsonIgnore
  private double medianDuration;
  @JsonIgnore
  private int frequency;

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

  public int getLabelValue() {
    return labelValue;
  }

  public void setLabelValue(int labelValue) {
    this.labelValue = labelValue;
  }

  public double getRelativeValue() {
    return relativeValue;
  }

  public void setRelativeValue(double relativeValue) {
    this.relativeValue = relativeValue;
  }

  public double getMedianDuration() {
    return medianDuration;
  }

  public void setMedianDuration(double medianDuration) {
    this.medianDuration = medianDuration;
  }

  public int getFrequency() {
    return frequency;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }
}
