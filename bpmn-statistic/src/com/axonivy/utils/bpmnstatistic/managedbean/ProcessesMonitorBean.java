package com.axonivy.utils.bpmnstatistic.managedbean;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.bpmnstatistic.bo.Node;
import com.axonivy.utils.bpmnstatistic.bo.ProcessMiningData;
import com.axonivy.utils.bpmnstatistic.bo.TimeFrame;
import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;
import com.axonivy.utils.bpmnstatistic.enums.AnalysisType;
import com.axonivy.utils.bpmnstatistic.enums.MonitorVersion;
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;
import com.axonivy.utils.bpmnstatistic.utils.DateUtils;
import com.axonivy.utils.bpmnstatistic.utils.JacksonUtils;
import com.axonivy.utils.bpmnstatistic.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.viewer.api.ProcessViewer;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@ManagedBean
@SessionScoped
public class ProcessesMonitorBean implements Serializable {
  private static final long serialVersionUID = 4601743634163868709L;
  private String JSON_FILE_FORMAT = "%s.json";
  private Map<String, List<IProcessWebStartable>> processesMap = new HashMap<>();
  private TimeIntervalFilter timeIntervalFilter;
  private String selectedProcessName;
  private String selectedModuleName;
  private String selectedProcessDiagramUrl;
  private String selectedPid;
  private Integer totalFrequency = 0;
  private List<Node> nodes;
  private ProcessMiningData processMiningData;
  private AnalysisType selectedAnalysisType;
  private String selectedMonitorVersion;

  @PostConstruct
  private void init() {
    processesMap = ProcessUtils.getProcessesWithPmv();
    selectedAnalysisType = AnalysisType.FREQUENCY;
    nodes = new ArrayList<>();
  }

  public void onChangeSelectedModule() {
    if (StringUtils.isBlank(selectedModuleName)) {
      selectedModuleName = null;
      selectedProcessName = null;
      selectedProcessDiagramUrl = null;
      totalFrequency = 0;
    }
  }

  public List<String> getVersions() {
    return Arrays.stream(MonitorVersion.values()).map(MonitorVersion::getCmsName).toList();
  }

  public void onChangeSelectedProcess() {
    loadNodes();
  }

  public void onChangeSelectedAnalysisType() {
    loadNodes();
  }

  public void showStatisticData() {
    if (StringUtils.isNoneBlank(selectedPid)) {
      ProcessesMonitorUtils.showStatisticData(selectedPid);
      ProcessesMonitorUtils.showAdditionalInformation(String.valueOf(totalFrequency),
          DateUtils.getDateAsString(timeIntervalFilter.getFrom()),
          DateUtils.getDateAsString(timeIntervalFilter.getTo()));
    }
  }

  public void updateDataOnChangingFilter() throws ParseException {
    if (StringUtils.isBlank(selectedModuleName) || StringUtils.isBlank(selectedProcessName)) {
      return;
    }
    var parameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String from = parameterMap.get(ProcessMonitorConstants.FROM);
    String to = parameterMap.get(ProcessMonitorConstants.TO);
    timeIntervalFilter.setFrom(DateUtils.parseDateFromString(from));
    timeIntervalFilter.setTo(DateUtils.parseDateFromString(to));
    ProcessesMonitorUtils.showAdditionalInformation(String.valueOf(totalFrequency),
        DateUtils.getDateAsString(timeIntervalFilter.getFrom()), DateUtils.getDateAsString(timeIntervalFilter.getTo()));
    onChangeSelectedProcess();
  }

  private void loadNodes() {
    totalFrequency = 0;
    if (StringUtils.isNotBlank(selectedProcessName) && StringUtils.isNotBlank(selectedModuleName)
        && selectedAnalysisType != null) {
      if (timeIntervalFilter == null) {
        timeIntervalFilter = TimeIntervalFilter.getDefaultFilterSet();
      }
      Optional.ofNullable(getSelectedIProcessWebStartable()).ifPresent(process -> {
        processMiningData = new ProcessMiningData();
        selectedPid = process.pid().getParent().toString();
        selectedProcessDiagramUrl = ProcessViewer.of(process).url().toWebLink().getAbsolute();
        processMiningData.setProcessId(selectedPid);
        processMiningData.setProcessName(selectedProcessName);
        processMiningData.setAnalysisType(selectedAnalysisType);
        TimeFrame timeFrame = new TimeFrame(timeIntervalFilter.getFrom(), timeIntervalFilter.getTo());
        processMiningData.setTimeFrame(timeFrame);
        if (MonitorVersion.INITIAL.getCmsName() == selectedMonitorVersion) {
          nodes = ProcessesMonitorUtils.filterInitialStatisticByInterval(getSelectedIProcessWebStartable(),
              timeIntervalFilter, selectedAnalysisType);
        } else {
          nodes = ProcessesMonitorUtils.newApproach(getSelectedIProcessWebStartable(), timeIntervalFilter, selectedAnalysisType);
        }
        for (Node node : nodes) {
          totalFrequency += node.getFrequency();
          node.setLabelValue(String.valueOf(Math.floor(Double.parseDouble(node.getLabelValue()) * 100) / 100));
        }
        processMiningData.setNodes(nodes);
        processMiningData.setNumberOfInstances(totalFrequency);
      });
    } else {
      nodes = new ArrayList<>();
    }
  }

  private IProcessWebStartable getSelectedIProcessWebStartable() {
    return CollectionUtils.emptyIfNull(processesMap.get(selectedModuleName)).stream()
        .filter(process -> process.getDisplayName().equalsIgnoreCase(selectedProcessName)).findAny().orElse(null);
  }

  public StreamedContent downloadFile() {
    try {
      String jsonString = JacksonUtils.convertObjectToJSONString(processMiningData);
      byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);
      InputStream inputStream = new ByteArrayInputStream(jsonBytes);
      StreamedContent file = DefaultStreamedContent.builder()
          .name(String.format(JSON_FILE_FORMAT, processMiningData.getProcessId()))
          .contentType(MediaType.APPLICATION_JSON).stream(() -> inputStream).build();
      return file;
    } catch (Exception e) {
      Ivy.log().error(e);
      return null;
    }
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

  public String getSelectedMonitorVersion() {
    return selectedMonitorVersion;
  }

  public void setSelectedMonitorVersion(String seletedMonitorVersion) {
    this.selectedMonitorVersion = seletedMonitorVersion;
  }
}
