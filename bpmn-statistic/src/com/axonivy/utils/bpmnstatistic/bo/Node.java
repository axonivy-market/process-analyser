package com.axonivy.utils.bpmnstatistic.bo;

import com.axonivy.utils.bpmnstatistic.enums.NodeType;

public class Node {
  @Override
	public String toString() {
		return "Node [type=" + type + ", id=" + id + ", label=" + label + ", labelValue=" + labelValue
				+ ", relativeValue=" + relativeValue + ", medianDuration=" + medianDuration + ", frequency=" + frequency
				+ "]";
	}

  private NodeType type;
  private String id;
  private String label;
  private String labelValue= "0";
  private double relativeValue;
  private double medianDuration;
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

  public String getLabelValue() {
    return labelValue;
  }

  public void setLabelValue(String labelValue) {
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
