package com.axonivy.utils.bpmnstatistic.bo;

import java.util.List;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;

@SuppressWarnings("restriction")
public class AlternativePath {
  private SequenceFlow originFlow;
  private List<String> nodeIdsInPath;
  private String endProcessElementId;
  private Long taskSwitchEventIdOnPath;
  
  public Long getTaskSwitchEventIdOnPath() {
    return taskSwitchEventIdOnPath;
  }

  public void setTaskSwitchEventIdOnPath(Long taskSwitchEventIdOnPath) {
    this.taskSwitchEventIdOnPath = taskSwitchEventIdOnPath;
  }

  public SequenceFlow getOriginFlow() {
    return originFlow;
  }

  public void setOriginFlow(SequenceFlow originFlow) {
    this.originFlow = originFlow;
  }

  public String getEndProcessElementId() {
    return endProcessElementId;
  }

  public void setEndProcessElementId(String endProcessElementId) {
    this.endProcessElementId = endProcessElementId;
  }

  public List<String> getNodeIdsInPath() {
    return nodeIdsInPath;
  }

  public void setNodeIdsInPath(List<String> nodeIdsInPath) {
    this.nodeIdsInPath = nodeIdsInPath;
  }
}
