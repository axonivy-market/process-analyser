package com.axonivy.utils.process.analyzer.bo;

import java.util.List;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;

@SuppressWarnings("restriction")
public class AlternativePath {
  private SequenceFlow originFlow;
  private List<String> nodeIdsInPath;
  private String taskSwitchEventIdOnPath;

  public String getTaskSwitchEventIdOnPath() {
    return taskSwitchEventIdOnPath;
  }

  public void setTaskSwitchEventIdOnPath(String taskSwitchEventIdOnPath) {
    this.taskSwitchEventIdOnPath = taskSwitchEventIdOnPath;
  }

  public SequenceFlow getOriginFlow() {
    return originFlow;
  }

  public void setOriginFlow(SequenceFlow originFlow) {
    this.originFlow = originFlow;
  }

  public List<String> getNodeIdsInPath() {
    return nodeIdsInPath;
  }

  public void setNodeIdsInPath(List<String> nodeIdsInPath) {
    this.nodeIdsInPath = nodeIdsInPath;
  }
}
