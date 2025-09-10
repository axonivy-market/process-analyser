package com.axonivy.solutions.process.analyser.bo;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

public class TaskPath {
  private String taskUUID;
  private List<String> nodesInPath;
  private List<Path> paths;

  public TaskPath() { }

  public TaskPath(String taskUUID, List<Path> paths) {
    this.taskUUID = taskUUID;
    this.paths = paths;
  }

  public TaskPath(String taskUUID, List<String> nodesInPath, List<Path> paths) {
    this.taskUUID = taskUUID;
    this.nodesInPath = nodesInPath;
    this.paths = paths;
  }

  public String getTaskUUID() {
    return taskUUID;
  }

  public void setTaskUUID(String taskUUID) {
    this.taskUUID = taskUUID;
  }

  public boolean isFinished() {
    return CollectionUtils.isEmpty(paths) ? false : paths.stream().anyMatch(Path::isFound);
  }

  public List<String> getNodesInPath() {
    return nodesInPath;
  }

  public void setNodesInPath(List<String> nodesInPath) {
    this.nodesInPath = nodesInPath;
  }

  public List<Path> getPaths() {
    return paths;
  }

  public void setPaths(List<Path> paths) {
    this.paths = paths;
  }
}
