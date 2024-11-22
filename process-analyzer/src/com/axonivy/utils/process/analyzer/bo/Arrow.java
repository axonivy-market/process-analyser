package com.axonivy.utils.process.analyzer.bo;

public class Arrow {
  private String arrowId;
  private float ratio;
  private String label;
  private int frequency;
  private double medianDuration;

  public Arrow(String id, Float ratio, String label) {
    this.arrowId = id;
    this.ratio = ratio;
    this.label = label;
  }

  public Arrow() {
  }

  public String getArrowId() {
    return arrowId;
  }

  public void setArrowId(String arrowId) {
    this.arrowId = arrowId;
  }

  public float getRatio() {
    return ratio;
  }

  public void setRatio(float ratio) {
    this.ratio = ratio;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public int getFrequency() {
    return frequency;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }

  public double getMedianDuration() {
    return medianDuration;
  }

  public void setMedianDuration(double medianDuration) {
    this.medianDuration = medianDuration;
  }

  @Override
  public String toString() {
    return "Arrow [arrowId=" + arrowId + ", ratio=" + ratio + ", label=" + label + ", frequency=" + frequency
        + ", medianDuration=" + medianDuration + "]";
  }
}
