package com.axonivy.utils.process.analyzer.managedbean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.process.analyzer.bo.Node;
import com.axonivy.utils.process.analyzer.bo.ProcessMiningData;
import com.axonivy.utils.process.analyzer.bo.TimeFrame;
import com.axonivy.utils.process.analyzer.bo.TimeIntervalFilter;
import com.axonivy.utils.process.analyzer.constants.ProcessAnalyticsConstants;
import com.axonivy.utils.process.analyzer.enums.KpiType;
import com.axonivy.utils.process.analyzer.internal.ProcessUtils;
import com.axonivy.utils.process.analyzer.utils.DateUtils;
import com.axonivy.utils.process.analyzer.utils.JacksonUtils;
import com.axonivy.utils.process.analyzer.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.location.IParser.ParseException;
import ch.ivyteam.ivy.process.rdm.IProcess;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@ManagedBean
@ViewScoped
public class ProcessesAnalyticsBean {
  private Map<String, List<IProcessWebStartable>> processesMap = new HashMap<>();
  private String selectedProcess;
  private String selectedModule;
  private KpiType selectedKpiType;
  private String applicationName;
  private String targetHost;
  private String targetApplicationName;
  private List<Node> nodes;
  private TimeIntervalFilter timeIntervalFilter;
  private ProcessMiningData processMiningData;
  private String selectedPid;
  private String miningUrl;
  private ContentObject processMiningDataJsonFile;
  private String bpmnIframeSourceUrl;

  @PostConstruct
  private void init() {
    processesMap = ProcessUtils.getProcessesWithPmv();
    applicationName = Ivy.request().getApplication().getName();
    targetHost = Ivy.html().applicationHomeLink().toAbsoluteUri().getAuthority();
    targetApplicationName = applicationName;
    setNodes(new ArrayList<>());
    processMiningDataJsonFile = ContentManagement.cms(IApplication.current()).root().child()
        .folder(ProcessAnalyticsConstants.PROCESS_ANALYZER_CMS_PATH).child()
        .file(ProcessAnalyticsConstants.DATA_CMS_PATH, ProcessAnalyticsConstants.JSON_EXTENSION);
    miningUrl = processMiningDataJsonFile.uri();
    timeIntervalFilter = TimeIntervalFilter.getDefaultFilterSet();
  }

  public KpiType[] getKpiTypes() {
    return KpiType.values();
  }

  public List<String> getAvailableProcesses() {
    if (StringUtils.isBlank(selectedModule)) {
      return new ArrayList<>();
    }
    return processesMap.get(selectedModule).stream().map(IWebStartable::getDisplayName).collect(Collectors.toList());
  }

  public Set<String> getAvailableModules() {
    return processesMap.keySet();
  }

  public void onModuleSelect() {
    if (StringUtils.isBlank(selectedModule)) {
      selectedProcess = null;
    }
    resetStatisticValue();
  }

  public void onProcessSelect() {
    resetStatisticValue();
  }

  public void onKpiTypeSelect() {
    resetStatisticValue();
  }

  private void resetStatisticValue() {
    processMiningData = null;
    nodes = new ArrayList<>();
    bpmnIframeSourceUrl = StringUtils.EMPTY;
  }

  public void updateDataOnChangingFilter() throws ParseException {
    var parameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String from = parameterMap.get(ProcessAnalyticsConstants.FROM);
    String to = parameterMap.get(ProcessAnalyticsConstants.TO);
    timeIntervalFilter.setFrom(DateUtils.parseDateFromString(from));
    timeIntervalFilter.setTo(DateUtils.parseDateFromString(to));
  }

  public void onShowStatisticBtnClick() {
    loadNodes();
    updateProcessMiningDataJson();
    updateBpmnIframeSourceUrl();
  }

  private IProcessWebStartable getSelectedIProcessWebStartable() {
    return CollectionUtils.emptyIfNull(processesMap.get(selectedModule)).stream()
        .filter(process -> process.getDisplayName().equalsIgnoreCase(selectedProcess)).findAny().orElse(null);
  }

  private void updateBpmnIframeSourceUrl() {
    String processFilePath = getSelectedIProcessWebStartable().getId().replace(
        String.format(ProcessAnalyticsConstants.MODULE_PATH, targetApplicationName, selectedModule), StringUtils.EMPTY);
    int lastSlashIndex = processFilePath.lastIndexOf(ProcessAnalyticsConstants.SLASH);

    if (lastSlashIndex != StringUtils.INDEX_NOT_FOUND) {
      processFilePath = processFilePath.substring(0, lastSlashIndex) + IProcess.PROCESSFILE_EXTENSION;
    }
    bpmnIframeSourceUrl = String.format(ProcessAnalyticsConstants.PROCESS_ANALYZER_SOURCE_URL_PATTERN, applicationName,
        targetHost, targetApplicationName, selectedModule, processFilePath);
  }

  private void loadNodes() {
    if (StringUtils.isNotBlank(selectedProcess) && StringUtils.isNotBlank(selectedModule) && selectedKpiType != null) {
      Optional.ofNullable(getSelectedIProcessWebStartable()).ifPresent(process -> {
        int totalFrequency = 0;
        processMiningData = new ProcessMiningData();
        selectedPid = process.pid().getParent().toString();
        processMiningData.setProcessId(selectedPid);
        processMiningData.setProcessName(selectedProcess);
        processMiningData.setKpiType(selectedKpiType);
        TimeFrame timeFrame = new TimeFrame(timeIntervalFilter.getFrom(), timeIntervalFilter.getTo());
        processMiningData.setTimeFrame(timeFrame);
        nodes = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(
            getSelectedIProcessWebStartable(), timeIntervalFilter, selectedKpiType);
        for (Node node : nodes) {
          totalFrequency += node.getFrequency();
        }
        processMiningData.setNodes(nodes);
        processMiningData.setNumberOfInstances(totalFrequency);
      });
    } else {
      nodes = new ArrayList<>();
    }
  }

  private void updateProcessMiningDataJson() {
    String jsonString = JacksonUtils.convertObjectToJSONString(processMiningData);
    processMiningDataJsonFile.value().get(ProcessAnalyticsConstants.EN_CMS_LOCALE).write().string(jsonString);
  }

  public String generateNameOfExcelFile() {
    return StringUtils.isNotBlank(selectedProcess)
        ? String.format(ProcessAnalyticsConstants.ANALYSIS_EXCEL_FILE_PATTERN, selectedProcess)
        : StringUtils.EMPTY;
  }

  public String getSelectedProcess() {
    return selectedProcess;
  }

  public void setSelectedProcess(String selectedProcess) {
    this.selectedProcess = selectedProcess;
  }

  public String getSelectedModule() {
    return selectedModule;
  }

  public void setSelectedModule(String selectedModule) {
    this.selectedModule = selectedModule;
  }

  public KpiType getSelectedKpiType() {
    return selectedKpiType;
  }

  public void setSelectedKpiType(KpiType selectedKpiType) {
    this.selectedKpiType = selectedKpiType;
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  public String getMiningUrl() {
    return miningUrl;
  }

  public void setMiningUrl(String miningUrl) {
    this.miningUrl = miningUrl;
  }

  public boolean isShowStatisticBtnDisabled() {
    return StringUtils.isAnyBlank(selectedModule, selectedProcess) || selectedKpiType == null;
  }

  public String getBpmnIframeSourceUrl() {
    return bpmnIframeSourceUrl;
  }
}
