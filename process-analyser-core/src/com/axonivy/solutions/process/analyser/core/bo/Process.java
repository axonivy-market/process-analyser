package com.axonivy.solutions.process.analyser.core.bo;

import java.util.List;

import ch.ivyteam.ivy.application.IProcessModelVersion;

public class Process {

  private String id;
  private long pmvId;
  private String pmvName;
  private IProcessModelVersion pmv;
  private String requestPath;
  private String name;
  private String projectRelativePath;
  private List<StartElement> startElements;

  public Process() { }

  public Process(String id, String name, List<StartElement> startElements) {
    this.id = id;
    this.name = name;
    this.startElements = startElements;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getPmvId() {
    return pmvId;
  }

  public void setPmvId(long pmvId) {
    this.pmvId = pmvId;
  }

  public String getPmvName() {
    return pmvName;
  }

  public void setPmvName(String pmvName) {
    this.pmvName = pmvName;
  }

  public IProcessModelVersion getPmv() {
    return pmv;
  }

  public void setPmv(IProcessModelVersion pmv) {
    this.pmv = pmv;
  }

  public String getRequestPath() {
    return requestPath;
  }

  public void setRequestPath(String requestPath) {
    this.requestPath = requestPath;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getProjectRelativePath() {
    return projectRelativePath;
  }

  public void setProjectRelativePath(String projectRelativePath) {
    this.projectRelativePath = projectRelativePath;
  }

  public List<StartElement> getStartElements() {
    return startElements;
  }

  public void setStartElements(List<StartElement> startElements) {
    this.startElements = startElements;
  }

  @Override
  public String toString() {
    final var pattern = "Process: { id: %s/ name: %s/ pmv: %s/ requestPath: %s/ projectRelativePath: %s/ startElements: %s }";
    return pattern.formatted(id, name, pmvName, requestPath, projectRelativePath, startElements);
  }
}
