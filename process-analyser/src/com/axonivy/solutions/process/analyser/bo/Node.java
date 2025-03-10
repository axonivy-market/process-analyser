package com.axonivy.solutions.process.analyser.bo;

import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.serializer.LabelValueSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Node {
  private NodeType type;
  private String id;
  @JsonIgnore
  private String label;
  @JsonSerialize(using = LabelValueSerializer.class)
  private float labelValue;
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

  public float getLabelValue() {
    return labelValue;
  }

  public void setLabelValue(float labelValue) {
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
