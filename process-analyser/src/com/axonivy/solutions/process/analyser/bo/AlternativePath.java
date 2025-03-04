package com.axonivy.solutions.process.analyser.bo;

import java.util.List;

public class AlternativePath {
  private String originFlowId;
  private List<String> nodeIdsInPath;
  private String taskSwitchEventIdOnPath;

  public String getTaskSwitchEventIdOnPath() {
    return taskSwitchEventIdOnPath;
  }

  public void setTaskSwitchEventIdOnPath(String taskSwitchEventIdOnPath) {
    this.taskSwitchEventIdOnPath = taskSwitchEventIdOnPath;
  }

  public String getOriginFlowId() {
    return originFlowId;
  }

  public void setOriginFlowId(String originFlowId) {
    this.originFlowId = originFlowId;
  }

  public List<String> getNodeIdsInPath() {
    return nodeIdsInPath;
  }

  public void setNodeIdsInPath(List<String> nodeIdsInPath) {
    this.nodeIdsInPath = nodeIdsInPath;
  }
}
