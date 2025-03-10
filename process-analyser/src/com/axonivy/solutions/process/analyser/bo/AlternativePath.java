package com.axonivy.solutions.process.analyser.bo;

import java.util.List;

public class AlternativePath {
  private List<String> precedingFlowIds;
  private List<String> nodeIdsInPath;
  private String taskSwitchEventIdOnPath;
  private boolean isPathFromAlternativeEnd;

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

  public boolean isPathFromAlternativeEnd() {
    return isPathFromAlternativeEnd;
  }

  public void setPathFromAlternativeEnd(boolean isPathFromAlternativeEnd) {
    this.isPathFromAlternativeEnd = isPathFromAlternativeEnd;
  }

  public List<String> getPrecedingFlowIds() {
    return precedingFlowIds;
  }

  public void setPrecedingFlowIds(List<String> precedingFlowIds) {
    this.precedingFlowIds = precedingFlowIds;
  }
}
