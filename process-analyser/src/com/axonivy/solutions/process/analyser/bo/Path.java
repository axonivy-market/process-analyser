package com.axonivy.solutions.process.analyser.bo;

import java.util.List;

import com.axonivy.solutions.process.analyser.enums.PathStatus;

public class Path {

  private PathStatus status;
  private String startPathId;
  private String endPathId;
  private List<String> nodesInPath;

  public Path() { }

  public Path(List<String> nodesInPath) {
    this.nodesInPath = nodesInPath;
  }

  public Path(String startPathId, List<String> nodesInPath) {
    this.startPathId = startPathId;
    this.nodesInPath = nodesInPath;
  }

  public Path(PathStatus status, String startPathId, String endPathId, List<String> nodesInPath) {
    this.status = status;
    this.startPathId = startPathId;
    this.endPathId = endPathId;
    this.nodesInPath = nodesInPath;
  }

  public boolean isFound() {
    return this.status == PathStatus.FOUND;
  }

  public PathStatus getStatus() {
    return status;
  }

  public void setStatus(PathStatus status) {
    this.status = status;
  }

  public String getStartPathId() {
    return startPathId;
  }

  public void setStartPathId(String startPathId) {
    this.startPathId = startPathId;
  }

  public String getEndPathId() {
    return endPathId;
  }

  public void setEndPathId(String endPathId) {
    this.endPathId = endPathId;
  }

  public List<String> getNodesInPath() {
    return nodesInPath;
  }

  public void setNodesInPath(List<String> nodesInPath) {
    this.nodesInPath = nodesInPath;
  }

  @Override
  public String toString() {
    final var pattern = "Path: { StartPathId: %s / EndPathId: %s / NodesInPath: %s}";
    return pattern.formatted(startPathId, endPathId, nodesInPath);
  }
}
