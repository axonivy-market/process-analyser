package com.axonivy.utils.bpmnstatistic.managedbean;

import java.text.ParseException;
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

import com.axonivy.utils.bpmnstatistic.bo.Arrow;
import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;
import com.axonivy.utils.bpmnstatistic.utils.DateUtils;
import com.axonivy.utils.bpmnstatistic.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.process.viewer.api.ProcessViewer;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@ManagedBean
@ViewScoped
public class ProcessesMonitorBean {
  private Map<String, List<IProcessWebStartable>> processesMap = new HashMap<>();
  private TimeIntervalFilter timeIntervalFilter;
  private String selectedProcessName;
  private String selectedModuleName;
  private String selectedProcessDiagramUrl;
  private String selectedPid;
  private List<Arrow> arrows;
  private Integer totalFrequency = 0;

  @PostConstruct
  private void init() {
    arrows = new ArrayList<>();
    processesMap = ProcessUtils.getProcessesWithPmv();
  }

  public void onChangeSelectedModule() {
    if (StringUtils.isBlank(selectedModuleName)) {
      selectedModuleName = null;
      selectedProcessName = null;
      arrows = new ArrayList<>();
      selectedProcessDiagramUrl = null;
      totalFrequency = 0;
    }
  }

  public void onChangeSelectedProcess() {
    totalFrequency = 0;
    if (StringUtils.isNotBlank(selectedProcessName) && StringUtils.isNotBlank(selectedModuleName)) {
      if (timeIntervalFilter == null) {
        timeIntervalFilter = TimeIntervalFilter.getDefaultFilterSet();
      }
      Optional.ofNullable(getSelectedIProcessWebStartable()).ifPresent(process -> {
        selectedPid = process.pid().getParent().toString();
        selectedProcessDiagramUrl = ProcessViewer.of(process).url().toWebLink().getAbsolute();
        arrows = ProcessesMonitorUtils.filterStatisticByInterval(getSelectedIProcessWebStartable(), timeIntervalFilter);
        for (Arrow arrow : arrows) {
          arrow.setMedianDuration(Math.floor(arrow.getMedianDuration() * 100) / 100);
          arrow.setRatio((float) (Math.floor(arrow.getRatio() * 100) / 100));
          totalFrequency += arrow.getFrequency();
        }
      });
    }
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
    onChangeSelectedProcess();
  }

  private IProcessWebStartable getSelectedIProcessWebStartable() {
    return CollectionUtils.emptyIfNull(processesMap.get(selectedModuleName)).stream()
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

  public List<Arrow> getArrows() {
    return arrows;
  }

  public void setArrows(List<Arrow> arrows) {
    this.arrows = arrows;
  }
}
