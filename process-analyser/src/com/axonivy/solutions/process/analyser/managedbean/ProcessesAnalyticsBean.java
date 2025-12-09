package com.axonivy.solutions.process.analyser.managedbean;

import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.DATA_CMS_PATH;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.EN_CMS_LOCALE;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.FROM;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.JSON_EXTENSION;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.PROCESS_ANALYSER_CMS_PATH;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.TO;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.UPDATE_IFRAME_SOURCE_METHOD_CALL;
import static com.axonivy.solutions.process.analyser.core.constants.CoreConstants.HYPHEN_SIGN;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.primefaces.PF;

import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.ProcessMiningData;
import com.axonivy.solutions.process.analyser.bo.ProcessViewerConfig;
import com.axonivy.solutions.process.analyser.bo.TimeFrame;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.constants.ProcessAnalyticViewComponentId;
import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.bo.StartElement;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.service.IvyTaskOccurrenceService;
import com.axonivy.solutions.process.analyser.utils.DateUtils;
import com.axonivy.solutions.process.analyser.utils.FacesContexts;
import com.axonivy.solutions.process.analyser.utils.JacksonUtils;
import com.axonivy.solutions.process.analyser.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.ICase;

@ManagedBean
@ViewScoped
public class ProcessesAnalyticsBean {
  private static final String SUB_PROCESS_CALL_PID_PARAM_NAME = "subProcessCallPid";
  private List<Node> analyzedNode;
  private List<Node> filteredNodes;
  private TimeIntervalFilter timeIntervalFilter;
  private ProcessMiningData processMiningData;
  private String selectedPid;
  private String miningUrl;
  private ContentObject processMiningDataJsonFile;
  private boolean isIncludingRunningCases;

  private MasterDataBean masterDataBean;
  private ProcessViewerBean viewerBean;
  private ColorPickerBean colorPickerBean;
  private CustomFilterBean customFilterBean;
  private boolean isWidgetMode;

  @PostConstruct
  private void init() {
    initRelatedBeans();
    initDefaultVariableValue();
    initSelectedValueFromUserProperty();
  }

  private void initRelatedBeans() {
    viewerBean = FacesContexts.evaluateValueExpression("#{processViewerBean}", ProcessViewerBean.class);
    colorPickerBean = FacesContexts.evaluateValueExpression("#{colorPickerBean}", ColorPickerBean.class);
    masterDataBean = FacesContexts.evaluateValueExpression("#{masterDataBean}", MasterDataBean.class);
    customFilterBean = FacesContexts.evaluateValueExpression("#{customFilterBean}", CustomFilterBean.class);
  }

  private void initDefaultVariableValue() {
    isWidgetMode = masterDataBean.isWidgetMode();
    processMiningDataJsonFile = ContentManagement.cms(IApplication.current()).root().child().folder(PROCESS_ANALYSER_CMS_PATH).child()
        .file(DATA_CMS_PATH, JSON_EXTENSION);
    miningUrl = processMiningDataJsonFile.uri();
    timeIntervalFilter = TimeIntervalFilter.getDefaultFilterSet();
  }

  private void initSelectedValueFromUserProperty() {
//    ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
    // Early escapes if not in widget mode
    if (!masterDataBean.isWidgetMode()) {
      return;
    }
//    selectedModule = persistedConfig.getWidgetSelectedModule();
//    avaiableProcesses = ProcessUtils.getAllProcessByModule(this.selectedModule, this.selectedPMV);
//    isMergeProcessStarts = BooleanUtils.isTrue(persistedConfig.getWidgetMergedProcessStart());
//    isIncludingRunningCases = BooleanUtils.isTrue(persistedConfig.getWidgetIncludeRunningCase());
//    String selectedKpiTypeName = persistedConfig.getWidgetSelectedKpi();
//    String selectedProcessAnalyzerId = persistedConfig.getWidgetSelectedProcessAnalyzer();
//    if (StringUtils.isNoneBlank(selectedModule, selectedProcessAnalyzerId)) {
//      selectedProcessAnalyser = initSelectedProcessAnalyser(selectedProcessAnalyzerId);
//    }
//    if (StringUtils.isNotBlank(selectedKpiTypeName)) {
//      selectedKpiType = KpiType.valueOf(selectedKpiTypeName);
//    }
    colorPickerBean.initBean(masterDataBean.getSelectedKpiType(), masterDataBean.isWidgetMode());
    updateDiagramAndStatistic();
  }
//
//  private ProcessAnalyser initSelectedProcessAnalyser(String selectedProcessAnalyzerId) {
//    ProcessAnalyser persistedProcessAnalyser = new ProcessAnalyser();
//    String[] parts = selectedProcessAnalyzerId.split(HYPHEN_SIGN, 2);
//    if (parts.length >= 1) {
//
//      String selectedProcessId = parts[0];
//      var selectedProcess = avaiableProcesses.stream()
//          .filter(process -> Strings.CS.equals(process.getId(), selectedProcessId))
//          .findAny()
//          .orElse(null);
//
//      if (selectedProcess == null) {
//        return null;
//      }
//
//      persistedProcessAnalyser.setProcess(selectedProcess);
//      String selectedStartPid = parts.length == 2 ? parts[1] : StringUtils.EMPTY;
//      if (StringUtils.isBlank(selectedStartPid)) {
//        return persistedProcessAnalyser;
//      }
//      var selectedProcessStart = selectedProcess.getStartElements().stream()
//          .filter(start -> Strings.CS.equals(start.getPid(), selectedStartPid)).findAny().orElse(null);
//
//      persistedProcessAnalyser.setStartElement(selectedProcessStart);
//    }
//    return persistedProcessAnalyser;
//  }

  public void onModuleSelect() {
    masterDataBean.onModuleSelect();
    if (isWidgetMode) {
      PF.current().ajax()
          .update(List.of(ProcessAnalyticViewComponentId.WIDGET_PROCESS_SELECTION_GROUP, ProcessAnalyticViewComponentId.PMV_GROUP));
      return;
    }
    PF.current().ajax().update(List.of(ProcessAnalyticViewComponentId.PROCESS_SELECTION_GROUP, ProcessAnalyticViewComponentId.PMV_GROUP));
    resetStatisticValue();
  }

  public void updateDataTable() {
    String subProcessCallPid =
        FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(SUB_PROCESS_CALL_PID_PARAM_NAME);
    if (StringUtils.isNotBlank(subProcessCallPid)) {
      updateDataTableWithNodesPrefix(subProcessCallPid);
      renderNodesByKPIType();
    }
  }

  private void updateDataTableWithNodesPrefix(String prefix) {
    filteredNodes = analyzedNode.stream().filter(node -> node.getId().startsWith(prefix)).collect(Collectors.toList());
  }

  public void prepareForExportingJPEG(boolean isResetView) {
    if (isResetView) {
      viewerBean.resetViewerSelection();
      refreshDiagramAndStatisticUI();
    }
  }

  public void onPmvSelect() {
    if (ObjectUtils.isEmpty(masterDataBean.getSelectedPMV())) {
      masterDataBean.setAvaiableProcesses(new ArrayList<>());
      masterDataBean.setSelectedProcessAnalyser(null);
    } else {
      masterDataBean
          .setAvaiableProcesses(ProcessUtils.getAllProcessByModule(masterDataBean.getSelectedModule(), masterDataBean.getSelectedPMV()));
    }
    if (ObjectUtils.isNotEmpty(masterDataBean.getSelectedProcessAnalyser())) {
      masterDataBean.setSelectedProcessAnalyser(ProcessesMonitorUtils.mappingProcessAnalyzerByProcesses(masterDataBean.getAvaiableProcesses(),
          masterDataBean.isMergeProcessStarts(), masterDataBean.getSelectedProcessAnalyser().getProcessKeyId()));
    }
    customFilterBean.updateCustomFilterPanel();
    updateDiagramAndStatistic();
    PF.current().ajax().update(ProcessAnalyticViewComponentId.PROCESS_SELECTION_GROUP);
  }

  public void onProcessSelect() {
    if (masterDataBean.isWidgetMode()) {
      String widgetSelectedProcessAnalyzer = getWidgetSelectedProcessAnalyzerKey();
      ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
      persistedConfig.setWidgetSelectedProcessAnalyzer(widgetSelectedProcessAnalyzer);
      ProcessesMonitorUtils.updateUserProperty(persistedConfig);
      return;
    }
    resetStatisticValue();
    if (masterDataBean.getSelectedProcessAnalyser() != null) {
      updateDiagramAndStatistic();
      customFilterBean.updateCustomFilterPanel();
    }
    refreshDiagramAndStatisticUI();
  }

  private String getWidgetSelectedProcessAnalyzerKey() {
    String selectedProcessId =
        Optional.ofNullable(masterDataBean.getSelectedProcessAnalyser().getProcess()).map(Process::getId).orElse(StringUtils.EMPTY);
    String selectedStartId =
        Optional.ofNullable(masterDataBean.getSelectedProcessAnalyser().getStartElement()).map(StartElement::getPid).orElse(StringUtils.EMPTY);
    String widgetSelectedProcessAnalyzer =
        masterDataBean.isMergeProcessStarts() ? selectedProcessId : String.join(HYPHEN_SIGN, selectedProcessId, selectedStartId);
    return widgetSelectedProcessAnalyzer;
  }

  public void onKpiTypeSelect() {
    if (masterDataBean.isWidgetMode()) {
      ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
      persistedConfig.setWidgetSelectedKpi(masterDataBean.getSelectedKpiType().name());
      ProcessesMonitorUtils.updateUserProperty(persistedConfig);
      return;
    }
    colorPickerBean.updateColorByKpiType(masterDataBean.getSelectedKpiType());
    refreshDiagramAndStatistic();
  }

  private void resetStatisticValue() {
    customFilterBean.resetCustomFieldFilterValues();
    processMiningData = null;
    filteredNodes = List.of();
    analyzedNode = List.of();
  }

  public List<CustomFieldFilter> getCaseAndTaskCustomFields() {
    if (masterDataBean.getSelectedProcessAnalyser() == null || masterDataBean.getSelectedProcessAnalyser().getProcess() == null) {
      return new ArrayList<>();
    }
    return customFilterBean.getCustomFieldsByType();
  }

  public void onChangeIncludingRunningCases() {
    if (masterDataBean.isWidgetMode()) {
      ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
      persistedConfig.setWidgetIncludeRunningCase(isIncludingRunningCases);
      ProcessesMonitorUtils.updateUserProperty(persistedConfig);
    }
    refreshDiagramAndStatistic();
  }

  public void onColorChange() {
    colorPickerBean.onColorChange();
    if (!masterDataBean.isWidgetMode()) {
      refreshDiagramAndStatistic();
    }
  }

  public void onChangeMergeProcessStarts() {
    if (masterDataBean.isWidgetMode()) {
      ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
      persistedConfig.setWidgetMergedProcessStart(masterDataBean.isMergeProcessStarts());
      ProcessesMonitorUtils.updateUserProperty(persistedConfig);
    }
    refreshDiagramAndStatistic();;
  }

  public void onColorModeChange() {
    colorPickerBean.onColorModeChange();
    if (!masterDataBean.isWidgetMode()) {
      refreshDiagramAndStatistic();
    }
  }

  public void updateDataOnChangingFilter() {
    var parameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String from = parameterMap.get(FROM);
    String to = parameterMap.get(TO);
    timeIntervalFilter.setFrom(DateUtils.parseDateFromString(from));
    timeIntervalFilter.setTo(DateUtils.parseDateFromString(to));
    if (!masterDataBean.isWidgetMode()) {
      resetStatisticValue();
      customFilterBean.updateCustomFilterPanel();
      refreshDiagramAndStatistic();
    }
  }

  public void updateDiagramAndStatistic() {
    if (masterDataBean.isStatisticReportRenderable()) {
      viewerBean.init(masterDataBean.getSelectedProcessAnalyser());
      loadNodes();
      updateProcessMiningDataJson();
      renderNodesByKPIType();
      PF.current().executeScript(UPDATE_IFRAME_SOURCE_METHOD_CALL);
    }
  }

  public void refreshDiagramAndStatisticUI() {
    PF.current().ajax().update(ProcessAnalyticViewComponentId.getDiagramAndStatisticComponentIds());
  }

  public void refreshDiagramAndStatistic() {
    updateDiagramAndStatistic();
    refreshDiagramAndStatisticUI();
  }

  private void loadNodes() {
    analyzedNode = new ArrayList<>();
    if (masterDataBean.getSelectedProcessAnalyser() == null) {
      resetStatisticValue();
      return;
    }
    selectedPid = masterDataBean.getSelectedProcessAnalyser().getProcess().getId();
    initializingProcessMiningData();
    if (haveMandatoryFieldsBeenFilled()) {
      List<ICase> cases = new ArrayList<>();
      boolean shouldIncludeRunningCasesByKpi = isIncludingRunningCases && !masterDataBean.getSelectedKpiType().isDescendantOf(KpiType.DURATION);
      if (masterDataBean.isMergeProcessStarts()) {
        List<Long> taskStartIds =
            masterDataBean.getSelectedProcessAnalyser().getProcess().getStartElements().stream().map(StartElement::getTaskStartId).toList();
        for (Long taskStartId : taskStartIds) {
          List<ICase> subCases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(taskStartId, timeIntervalFilter,
              customFilterBean.getSelectedCustomFilters(), shouldIncludeRunningCasesByKpi);
          if (CollectionUtils.isNotEmpty(subCases)) {
            cases.addAll(subCases);
          }
        }
      } else {
        Long taskStartId = masterDataBean.getSelectedProcessAnalyser().getStartElement().getTaskStartId();
        cases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(taskStartId, timeIntervalFilter,
            customFilterBean.getSelectedCustomFilters(), shouldIncludeRunningCasesByKpi);
      }
      if (CollectionUtils.isNotEmpty(cases)) {
        var role = masterDataBean.getSelectedRole();
        var tasks = cases.stream().flatMap(ivyCase -> ivyCase.tasks().all().stream())
            .filter(task -> StringUtils.isBlank(role) || Strings.CS.equals(task.getActivatorName(), role)).toList();
        customFilterBean
            .setCustomFieldsByType(IvyTaskOccurrenceService.getCaseAndTaskCustomFields(tasks, customFilterBean.getCustomFieldsByType()));
        analyzedNode = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(masterDataBean.getSelectedProcessAnalyser(),
            masterDataBean.getSelectedKpiType(), tasks);
        processMiningData.setNodes(analyzedNode);
        processMiningData.setNumberOfInstances(cases.size());
      }
    }
    updateDataTableWithNodesPrefix(ProcessUtils.getProcessPidFromElement(selectedPid));
  }

  private boolean haveMandatoryFieldsBeenFilled() {
    return StringUtils.isNoneBlank(masterDataBean.getSelectedModule()) && ObjectUtils.allNotNull(masterDataBean.getSelectedKpiType(),
        masterDataBean.getSelectedProcessAnalyser(), masterDataBean.getSelectedPMV());
  }

  private void updateProcessMiningDataJson() {
    String jsonString = JacksonUtils.convertObjectToJSONString(processMiningData);
    processMiningDataJsonFile.value().get(EN_CMS_LOCALE).write().string(jsonString);
  }

  private void initializingProcessMiningData() {
    Optional.ofNullable(masterDataBean.getSelectedProcessAnalyser().getProcess()).ifPresent(selectedProcess -> {
      List<Node> nodes = new ArrayList<>();
      nodes.add(new Node());
      processMiningData = new ProcessMiningData();
      processMiningData.setProcessId(selectedProcess.getId());
      processMiningData.setProcessName(selectedProcess.getName());
      processMiningData.setKpiType(masterDataBean.getSelectedKpiType());
      processMiningData.setTimeFrame(new TimeFrame(timeIntervalFilter.getFrom(), timeIntervalFilter.getTo()));
      processMiningData.setColors(colorPickerBean.getColorSegments());
      processMiningData.setTextColors(colorPickerBean.getTextColors());
      processMiningData.setNodes(nodes);
    });
  }

  public void renderNodesByKPIType() {
    if (masterDataBean.getSelectedKpiType() != null && masterDataBean.getSelectedKpiType().isDescendantOf(KpiType.DURATION)) {
      List<String> avaibleTaskIds =
          filteredNodes.stream().filter(node -> node.getType() == NodeType.ARROW).map(node -> node.getSourceNodeId()).toList();

      filteredNodes = filteredNodes.stream().filter(node -> node.getType() != NodeType.ARROW && avaibleTaskIds.contains(node.getId()))
          .collect(Collectors.toList());
    }
  }

  public boolean isMedianDurationColumnVisible() {
    return ProcessesMonitorUtils.isDuration(masterDataBean.getSelectedKpiType());
  }

  public String getCalulatedCellColor(Double value) {
    return colorPickerBean.getCalulatedCellColor(value);
  }

  public String getAccessibleTextColor(Double value) {
    return colorPickerBean.getAccessibleTextColor(value);
  }

  public KpiType getSelectedKpiType() {
    return masterDataBean.getSelectedKpiType();
  }

  public String getMiningUrl() {
    return miningUrl;
  }

  public void setMiningUrl(String miningUrl) {
    this.miningUrl = miningUrl;
  }

  public boolean isShowStatisticBtnDisabled() {
    return masterDataBean.isStatisticReportRenderable();
  }

  public TimeIntervalFilter getTimeIntervalFilter() {
    return timeIntervalFilter;
  }

  public void setTimeIntervalFilter(TimeIntervalFilter timeIntervalFilter) {
    this.timeIntervalFilter = timeIntervalFilter;
  }

  public List<Node> getFilteredNodes() {
    return filteredNodes;
  }

  public void setFilteredNodes(List<Node> filteredNodes) {
    this.filteredNodes = filteredNodes;
  }

  public boolean isIncludingRunningCases() {
    return isIncludingRunningCases;
  }

  public void setIncludingRunningCases(boolean isIncludingRunningCases) {
    this.isIncludingRunningCases = isIncludingRunningCases;
  }
}
