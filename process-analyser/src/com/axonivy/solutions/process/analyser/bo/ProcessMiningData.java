package com.axonivy.solutions.process.analyser.bo;

import java.util.List;

import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProcessMiningData {
  @JsonIgnore
  private String processId;
  private String processName;
  private KpiType kpiType;
  private List<Node> nodes;
  private TimeFrame timeFrame;
  private int numberOfInstances;

  public String getProcessId() {
    return processId;
  }

  public void setProcessId(String processId) {
    this.processId = processId;
  }

  public String getProcessName() {
    return processName;
  }

  public void setProcessName(String processName) {
    this.processName = processName;
  }

  public KpiType getKpiType() {
    return kpiType;
  }

  public void setKpiType(KpiType kpiType) {
    this.kpiType = kpiType;
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  public TimeFrame getTimeFrame() {
    return timeFrame;
  }

  public void setTimeFrame(TimeFrame timeFrame) {
    this.timeFrame = timeFrame;
  }

  public int getNumberOfInstances() {
    return numberOfInstances;
  }

  public void setNumberOfInstances(int numberOfInstances) {
    this.numberOfInstances = numberOfInstances;
  }
}
