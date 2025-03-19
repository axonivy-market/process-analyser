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
  private int labelValue;
  private double relativeValue;
  @JsonIgnore
  private double medianDuration;
  @JsonIgnore
  private int frequency;
  @JsonIgnore
  private boolean isAlternativeElement;
  @JsonIgnore
  private List<String> outGoingPathIds = new ArrayList<>();
  @JsonIgnore
  private List<String> inCommingPathIds = new ArrayList<>();
  @JsonIgnore
  private String targetNodeId;

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

  public boolean isAlternativeElement() {
    return isAlternativeElement;
  }

  public void setAlternativeElement(boolean isAlternativeElement) {
    this.isAlternativeElement = isAlternativeElement;
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
}
