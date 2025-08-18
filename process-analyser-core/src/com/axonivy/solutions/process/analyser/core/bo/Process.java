package com.axonivy.solutions.process.analyser.core.bo;

import ch.ivyteam.ivy.application.IProcessModelVersion;

public class Process {

  private String id;
  private long pmvId;
  private String pmvName;
  private IProcessModelVersion pmv;
  private String requestPath;
  private String name;
  private String projectRelativePath;

  public Process() { }

  public Process(String id, long pmvId, String pmvName, String requestPath, String name) {
    this.id = id;
    this.pmvId = pmvId;
    this.pmvName = pmvName;
    this.requestPath = requestPath;
    this.name = name;
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

  @Override
  public String toString() {
    final var pattern = "Process: { id: %s/ name: %s/ pmv: %s/ requestPath: %s/ projectRelativePath: %s }";
    return pattern.formatted(id, name, pmvName, requestPath, projectRelativePath);
  }
}
