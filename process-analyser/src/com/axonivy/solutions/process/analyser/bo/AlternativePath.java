package com.axonivy.solutions.process.analyser.bo;

import java.util.List;

import ch.ivyteam.ivy.process.model.element.ProcessElement;

@SuppressWarnings("restriction")
public class AlternativePath {
  private List<String> precedingFlowIds;
  private List<String> nodeIdsInPath;
  private String taskSwitchEventIdOnPath;
  private boolean isSolePathFromAlternativeEnd;
  private ProcessElement nestedSubProcessCall;
  private boolean isCallSubEndPath;
  private int numberOfRetries;

  public String getTaskSwitchEventIdOnPath() {
    return taskSwitchEventIdOnPath;
  }

  public void setTaskSwitchEventIdOnPath(String taskSwitchEventIdOnPath) {
    this.taskSwitchEventIdOnPath = taskSwitchEventIdOnPath;
  }

  public List<String> getNodeIdsInPath() {
    return nodeIdsInPath;
  }

  public void setNodeIdsInPath(List<String> nodeIdsInPath) {
    this.nodeIdsInPath = nodeIdsInPath;
  }

  public List<String> getPrecedingFlowIds() {
    return precedingFlowIds;
  }

  public void setPrecedingFlowIds(List<String> precedingFlowIds) {
    this.precedingFlowIds = precedingFlowIds;
  }

  public ProcessElement getNestedSubProcessCall() {
    return nestedSubProcessCall;
  }

  public void setNestedSubProcessCall(ProcessElement nestedSubProcessCall) {
    this.nestedSubProcessCall = nestedSubProcessCall;
  }

  public boolean isSolePathFromAlternativeEnd() {
    return isSolePathFromAlternativeEnd;
  }

  public void setSolePathFromAlternativeEnd(boolean isSolePathFromAlternativeEnd) {
    this.isSolePathFromAlternativeEnd = isSolePathFromAlternativeEnd;
  }

  public boolean isCallSubEndPath() {
    return isCallSubEndPath;
  }

  public void setCallSubEndPath(boolean isCallSubEndPath) {
    this.isCallSubEndPath = isCallSubEndPath;
  }

  public int getNumberOfRetries() {
    return numberOfRetries;
  }

  public void setNumberOfRetries(int numberOfRetries) {
    this.numberOfRetries = numberOfRetries;
  }

  @Override
  public String toString() {
    final var pattern = "AlternativePath{precedingFlowIds: %s / nodeIdsInPath: %s / taskSwitchEventIdOnPath: %s / numberOfRetries: %d}";
    return pattern.formatted(precedingFlowIds, nodeIdsInPath, taskSwitchEventIdOnPath, numberOfRetries);
  }
}
