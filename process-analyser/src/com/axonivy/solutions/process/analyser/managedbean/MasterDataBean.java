package com.axonivy.solutions.process.analyser.managedbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;

@ManagedBean
@ViewScoped
public class MasterDataBean implements Serializable {

  private static final long serialVersionUID = 1L;
  private Map<String, List<Process>> processesMap = new HashMap<>();

  @PostConstruct
  public void init() {
    processesMap = ProcessUtils.getProcessesWithPmv();
  }

  public List<Process> getAvailableProcesses(String selectedModule) {
    if (StringUtils.isBlank(selectedModule)) {
      return new ArrayList<>();
    }
    return processesMap.get(selectedModule);
  }

  public Map<String, List<Process>> getProcessesMap() {
    return processesMap;
  }

  public void setProcessesMap(Map<String, List<Process>> processesMap) {
    this.processesMap = processesMap;
  }
}
