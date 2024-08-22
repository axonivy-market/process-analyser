package com.axonivy.utils.bpmnstatistic.bo;

import java.util.List;

import com.axonivy.utils.bpmnstatistic.enums.AnalysisType;

public class ProcessMiningData {
  private String processId;
  private String processName;
  private AnalysisType analysisType;
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

  public AnalysisType getAnalysisType() {
    return analysisType;
  }

  public void setAnalysisType(AnalysisType analysisType) {
    this.analysisType = analysisType;
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
