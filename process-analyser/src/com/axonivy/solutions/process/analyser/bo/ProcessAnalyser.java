package com.axonivy.solutions.process.analyser.bo;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.bo.StartElement;

public class ProcessAnalyser {
  private Process process;
  private StartElement startElement;

  public ProcessAnalyser() { }

  public ProcessAnalyser(Process process) {
    this.process = process;
  }

  public ProcessAnalyser(Process process, StartElement startElement) {
    this.process = process;
    this.startElement = startElement;
  }

  public Process getProcess() {
    return process;
  }

  public void setProcess(Process process) {
    this.process = process;
  }

  public StartElement getStartElement() {
    return startElement;
  }

  public void setStartElement(StartElement startElement) {
    this.startElement = startElement;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || process == null) {
      return false;
    }
    ProcessAnalyser target = (ProcessAnalyser) obj;
    EqualsBuilder builder = new EqualsBuilder();
    builder.append(target.getProcess(), this.process);
    builder.append(target.getStartElement(), this.startElement);
    return builder.build();
  }

  @Override
  public int hashCode() {
    if (process == null) {
      return super.hashCode();
    }
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(process.getId());
    builder.append(process.getPmvId());
    builder.append(startElement == null ? "" : startElement.getPid());
    return builder.build();
  }
}
