package com.axonivy.utils.bpmnstatistic.managedbean;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.bpmnstatistic.bo.Node;
import com.axonivy.utils.bpmnstatistic.bo.ProcessMiningData;
import com.axonivy.utils.bpmnstatistic.bo.TimeFrame;
import com.axonivy.utils.bpmnstatistic.enums.AnalysisType;
import com.axonivy.utils.bpmnstatistic.utils.DateUtils;
import com.axonivy.utils.bpmnstatistic.utils.JacksonUtils;
import com.axonivy.utils.bpmnstatistic.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.viewer.api.ProcessViewer;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@ManagedBean
@ViewScoped
public class ProcessesMonitorBean {
  private Map<String, List<IProcessWebStartable>> processesMap = new HashMap<>();
  private String selectedProcessName;
  private String selectedModuleName;
  private String selectedProcessDiagramUrl;
  private String selectedPid;
  private List<Node> nodes;
  private ProcessMiningData processMiningData;
  private AnalysisType selectedAnalysisType;

  @PostConstruct
  private void init() {
    processesMap = ProcessesMonitorUtils.getProcessesWithPmv();
    selectedAnalysisType = AnalysisType.FREQUENCY;
  }

  public void onChangeSelectedModule() {
    if (StringUtils.isBlank(selectedModuleName)) {
      selectedModuleName = null;
      selectedProcessName = null;
      nodes = null;
      selectedProcessDiagramUrl = null;
    }
  }

  public void onChangeSelectedProcess() {
    loadNodes();
  }

  public void onChangeSelectedAnalysisType() {
    loadNodes();
  }

  private void loadNodes() {
    if (StringUtils.isNotBlank(selectedProcessName) && StringUtils.isNotBlank(selectedModuleName) && selectedAnalysisType != null) {
      Optional.ofNullable(getSelectedIProcessWebStartable()).ifPresent(process -> {
        processMiningData = new ProcessMiningData();
        selectedPid = process.pid().getParent().toString();
        selectedProcessDiagramUrl = ProcessViewer.of(process).url().toWebLink().getAbsolute();
        processMiningData.setProcessId(selectedPid);
        processMiningData.setProcessName(selectedProcessName);
        processMiningData.setAnalysisType(selectedAnalysisType);
        // Mock data for instances count from a time range. Remove it when implement
        // feature of time filter
        processMiningData.setNumberOfInstances(15);
        TimeFrame timeFrame = new TimeFrame(new Date(), new Date());
        processMiningData.setTimeFrame(timeFrame);
        nodes = ProcessesMonitorUtils.getStatisticData(getSelectedIProcessWebStartable(), selectedAnalysisType);
        processMiningData.setNodes(nodes);
        Ivy.log().info(JacksonUtils.convertObjectToJSONString(processMiningData));
      });
    } else {
      nodes = new ArrayList<>();
    }
  }

  public void showStatisticData() {
    if (StringUtils.isNoneBlank(selectedPid)) {
      ProcessesMonitorUtils.showStatisticData(selectedPid);
      ProcessesMonitorUtils.showAdditionalInformation(String.valueOf(processMiningData.getNumberOfInstances()),
          DateUtils.formatDate(processMiningData.getTimeFrame().getStart()),
          DateUtils.formatDate(processMiningData.getTimeFrame().getEnd()));
    }
  }

  private IProcessWebStartable getSelectedIProcessWebStartable() {
    return processesMap.get(selectedModuleName).stream()
        .filter(process -> process.getDisplayName().equalsIgnoreCase(selectedProcessName)).findAny().orElse(null);
  }

  public List<String> getProcessNames() {
    if (StringUtils.isBlank(selectedModuleName)) {
      return new ArrayList<>();
    }
    return processesMap.get(selectedModuleName).stream().map(IWebStartable::getDisplayName)
        .collect(Collectors.toList());
  }

  public Set<String> getPmvNames() {
    return processesMap.keySet();
  }

  public AnalysisType[] getAnalysisTypes() {
    return AnalysisType.values();
  }
 
  public void setSelectedProcessDiagramUrl(String selectedProcessDiagramUrl) {
    this.selectedProcessDiagramUrl = selectedProcessDiagramUrl;
  }

  public String getSelectedModuleName() {
    return selectedModuleName;
  }

  public void setSelectedModuleName(String selectedModuleName) {
    this.selectedModuleName = selectedModuleName;
  }

  public String getSelectedProcessName() {
    return selectedProcessName;
  }

  public void setSelectedProcessName(String selectedProcessName) {
    this.selectedProcessName = selectedProcessName;
  }

  public String getSelectedProcessDiagramUrl() {
    return selectedProcessDiagramUrl;
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  public AnalysisType getSelectedAnalysisType() {
    return selectedAnalysisType;
  }

  public void setSelectedAnalysisType(AnalysisType selectedAnalysisType) {
    this.selectedAnalysisType = selectedAnalysisType;
  }
}
