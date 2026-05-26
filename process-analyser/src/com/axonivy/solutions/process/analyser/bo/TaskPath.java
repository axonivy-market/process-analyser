package com.axonivy.solutions.process.analyser.bo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

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

  public List<String> getRelatedPathOfGivenPath(Path path) {
    List<String> relatedPaths = new ArrayList<>();
    if (path == null || paths == null) {
      return List.of();
    }
    String startPathId = path.getStartPathId();
    Path triggeredPath = null;
    do {
      triggeredPath = findTriggeredPath(startPathId);
      if (triggeredPath != null) {
        relatedPaths.addAll(0, triggeredPath.getNodesInPath());
        startPathId = triggeredPath.getStartPathId();
      }
    } while (triggeredPath != null);

    relatedPaths.addAll(path.getNodesInPath());
    return relatedPaths;
  }

  private Path findTriggeredPath(final String startPathId) {
    return paths.stream().filter(p -> ObjectUtils.isNotEmpty(p.getEndPathId()))
        .filter(p -> p.getEndPathId().equals(startPathId))
        .findAny().orElse(null);
  }

  @Override
  public String toString() {
    final var pattern = "TaskPath: { taskUUID: %s / NodesInPath: %s / Paths: %s}";
    return pattern.formatted(taskUUID, nodesInPath, paths);
  }
}
