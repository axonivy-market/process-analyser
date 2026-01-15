package com.axonivy.solutions.process.analyser.managedbean;

import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.DATA_CMS_PATH;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.EN_CMS_LOCALE;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.FROM;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.JSON_EXTENSION;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.PROCESS_ANALYSER_CMS_PATH;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.TO;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.UPDATE_IFRAME_SOURCE_METHOD_CALL;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.primefaces.PF;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.ProcessMiningData;
import com.axonivy.solutions.process.analyser.bo.TimeFrame;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.constants.AnalyserConstants;
import com.axonivy.solutions.process.analyser.constants.ProcessAnalyticViewComponentId;
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
import ch.ivyteam.ivy.security.ISecurityConstants;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;

@ManagedBean
@ViewScoped
public class ProcessesAnalyticsBean {
  private static final String SUB_PROCESS_CALL_PID_PARAM_NAME = "subProcessCallPid";
  private List<Node> analyzedNode;
  private List<Node> filteredNodes;
  private TreeNode<Object> filteredNodesTree;
  private TimeIntervalFilter timeIntervalFilter;
  private ProcessMiningData processMiningData;
  private String miningUrl;
  private ContentObject processMiningDataJsonFile;
  private MasterDataBean masterDataBean;
  private ProcessViewerBean viewerBean;
  private ColorPickerBean colorPickerBean;
  private CustomFilterBean customFilterBean;
  private boolean isWidgetMode;

  @PostConstruct
  private void init() {
    initRelatedBeans();
    initDefaultVariableValue();
    if (isWidgetMode) {
      masterDataBean.initSelectedValueFromUserProperty();
      colorPickerBean.initBean(masterDataBean.getSelectedKpiType(), isWidgetMode);
      updateDiagramAndStatistic();
    }
  }

  private void initRelatedBeans() {
    viewerBean = FacesContexts.evaluateValueExpression("#{processViewerBean}", ProcessViewerBean.class);
    colorPickerBean = FacesContexts.evaluateValueExpression("#{colorPickerBean}", ColorPickerBean.class);
    masterDataBean = FacesContexts.evaluateValueExpression("#{masterDataBean}", MasterDataBean.class);
    customFilterBean = FacesContexts.evaluateValueExpression("#{customFilterBean}", CustomFilterBean.class);
    filteredNodesTree = new DefaultTreeNode<Object>("root", null);
  }

  private void initDefaultVariableValue() {
    var isWidgetModeValue = FacesContexts.evaluateValueExpression("#{data.isWidgetMode}", Boolean.class);
    isWidgetMode = BooleanUtils.isTrue(isWidgetModeValue);
    processMiningDataJsonFile = ContentManagement.cms(IApplication.current()).root().child().folder(PROCESS_ANALYSER_CMS_PATH).child()
        .file(DATA_CMS_PATH, JSON_EXTENSION);
    miningUrl = processMiningDataJsonFile.uri();
    timeIntervalFilter = TimeIntervalFilter.getDefaultFilterSet();
  }

  /**
   * Update and refresh section
   */
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
    filteredNodesTree = ProcessesMonitorUtils.buildTreeFromNodes(filteredNodes);
  }


  private void updateProcessMiningDataJson() {
    String jsonString = JacksonUtils.convertObjectToJSONString(processMiningData);
    processMiningDataJsonFile.value().get(EN_CMS_LOCALE).write().string(jsonString);
  }

  public void updateDataOnChangingFilter() {
    var parameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String from = parameterMap.get(FROM);
    String to = parameterMap.get(TO);
    timeIntervalFilter.setFrom(DateUtils.parseDateFromString(from));
    timeIntervalFilter.setTo(DateUtils.parseDateFromString(to));
    if (!isWidgetMode) {
      resetStatisticValue();
      customFilterBean.updateCustomFilterPanel();
      updateAndRefreshDiagramAndStatistic();
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

  public void updateAndRefreshDiagramAndStatistic() {
    updateDiagramAndStatistic();
    refreshDiagramAndStatisticUI();
  }

  public void prepareForExportingJPEG(boolean isResetView) {
    if (isResetView) {
      viewerBean.resetViewerSelection();
      refreshDiagramAndStatisticUI();
    }
  }

  private void resetStatisticValue() {
    customFilterBean.resetCustomFieldFilterValues();
    processMiningData = null;
    filteredNodes = List.of();
    analyzedNode = List.of();
  }

  private void refreshAnalyzedData() {
      resetStatisticValue();
      refreshDiagramAndStatisticUI();
      customFilterBean.updateCustomFilterPanel();
  }

  /**
   * Action listener section
   */
  public void onModuleSelect() {
    masterDataBean.handleModuleChange();
    if (!isWidgetMode) {
      refreshAnalyzedData();
    }
  }

  public void onPmvSelect() {
    masterDataBean.handlePmvChange();
    if (!isWidgetMode) {
      refreshAnalyzedData();
    }
  }

  public void onProcessSelect() {
    masterDataBean.handleProcessChange();
    if (!isWidgetMode) {
      updateAndRefreshDiagramAndStatistic();
    }
  }

  public void onChangeIncludingRunningCases() {
    if (isWidgetMode) {
      ProcessesMonitorUtils
          .updateUserConfig(persistedConfig -> persistedConfig.setWidgetIncludeRunningCase(masterDataBean.isIncludingRunningCases()));
    }
    updateAndRefreshDiagramAndStatistic();
  }

  public void onColorChange() {
    colorPickerBean.onColorChange();
    if (!isWidgetMode) {
      updateAndRefreshDiagramAndStatistic();
    }
  }

  public void onChangeMergeProcessStarts() {
    masterDataBean.handleMergeProcessStartsChange();
    resetStatisticValue();
    onProcessSelect();
  }

  public void onColorModeChange() {
    colorPickerBean.onColorModeChange();
    if (!isWidgetMode) {
      updateAndRefreshDiagramAndStatistic();
    }
  }

  public void onKpiTypeSelect() {
    masterDataBean.handleKpiTypeChange();
    if (!isWidgetMode) {
      colorPickerBean.updateColorByKpiType(masterDataBean.getSelectedKpiType());
      updateAndRefreshDiagramAndStatistic();      
    }
  }

  public void onRoleSelect() {
    masterDataBean.handleRoleChange();
    if (!isWidgetMode) {
      updateAndRefreshDiagramAndStatistic();
    }
  }

  public List<CustomFieldFilter> getCaseAndTaskCustomFields() {
    if (masterDataBean.getSelectedProcessAnalyser() == null || masterDataBean.getSelectedProcessAnalyser().getProcess() == null) {
      return new ArrayList<>();
    }
    return customFilterBean.getCustomFieldsByType();
  }

  private boolean isTaskMatchRoleFilter(String taskActivatorName, String roleName) {
    return StringUtils.isBlank(roleName) || Strings.CS.equals(taskActivatorName, roleName)
        || (Strings.CS.equals(ISecurityConstants.TOP_LEVEL_ROLE_NAME, roleName) && taskActivatorName == null)
        || (Strings.CS.equals(ISecurityConstants.SYSTEM_USER_NAME, roleName)
            && Strings.CS.equals(AnalyserConstants.HASHTAG + ISecurityConstants.SYSTEM_USER_NAME, taskActivatorName));
  }

  private void loadNodes() {
    analyzedNode = new ArrayList<>();
    var selectedProcessAnalyser = masterDataBean.getSelectedProcessAnalyser();
    if (selectedProcessAnalyser == null) {
      resetStatisticValue();
      return;
    }
    initializingProcessMiningData();
    if (masterDataBean.isStatisticReportRenderable()) {
      List<ICase> cases = new ArrayList<>();
      boolean shouldIncludeRunningCasesByKpi = masterDataBean.isIncludingRunningCases() && !masterDataBean.isDurationKpiType();
      if (masterDataBean.isMergeProcessStarts()) {
        List<Long> taskStartIds =
            selectedProcessAnalyser.getProcess().getStartElements().stream().map(StartElement::getTaskStartId).toList();
        for (Long taskStartId : taskStartIds) {
          List<ICase> subCases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(taskStartId, timeIntervalFilter,
              customFilterBean.getSelectedCustomFilters(), shouldIncludeRunningCasesByKpi);
          if (CollectionUtils.isNotEmpty(subCases)) {
            cases.addAll(subCases);
          }
        }
      } else {
        Long taskStartId = selectedProcessAnalyser.getStartElement().getTaskStartId();
        cases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(taskStartId, timeIntervalFilter,
            customFilterBean.getSelectedCustomFilters(), shouldIncludeRunningCasesByKpi);
      }
      if (CollectionUtils.isNotEmpty(cases)) {
        String role = masterDataBean.getSelectedRole();
        List<ITask> tasks = cases.stream().flatMap(ivyCase -> ivyCase.tasks().all().stream())
            .filter(task -> isTaskMatchRoleFilter(task.getActivatorName(), role)).toList();
        customFilterBean.setCustomFieldsByType(
            IvyTaskOccurrenceService.getCaseAndTaskCustomFields(tasks, customFilterBean.getCustomFieldsByType()));
        analyzedNode = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(selectedProcessAnalyser,
            masterDataBean.getSelectedKpiType(), tasks);
        if (StringUtils.isNotBlank(role)) {
          analyzedNode = removeEmptyDataFromReport(analyzedNode, masterDataBean.isDurationKpiType());
        }
        processMiningData.setNodes(analyzedNode);
        processMiningData.setNumberOfInstances(cases.size());
      }
    }
    updateDataTableWithNodesPrefix(ProcessUtils.getProcessPidFromElement(selectedProcessAnalyser.getProcess().getId()));
  }

  private List<Node> removeEmptyDataFromReport(List<Node> nodes, boolean isDurationKPI) {
    Predicate<Node> filterPredicate = isDurationKPI ? node -> node.getMedianDuration() != 0
        : node -> node.getFrequency() != 0;
    return nodes.stream().filter(filterPredicate).collect(Collectors.toList());
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
    if (masterDataBean.isDurationKpiType()) {
      List<String> avaibleTaskIds =
          filteredNodes.stream().filter(node -> node.getType() == NodeType.ARROW).map(node -> node.getSourceNodeId()).toList();
      filteredNodes = filteredNodes.stream().filter(node -> node.getType() != NodeType.ARROW && avaibleTaskIds.contains(node.getId()))
          .collect(Collectors.toList());
    }
  }

  public boolean isMedianDurationColumnVisible() {
    return masterDataBean.isDurationKpiType();
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
    return !masterDataBean.isStatisticReportRenderable();
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

  public boolean isWidgetMode() {
    return isWidgetMode;
  }

  public void setWidgetMode(boolean isWidgetMode) {
    this.isWidgetMode = isWidgetMode;
  }

  public TreeNode<Object> getFilteredNodesTree() {
    return filteredNodesTree;
  }

  public void setFilteredNodesTree(TreeNode<Object> filteredNodesTree) {
    this.filteredNodesTree = filteredNodesTree;
  }
}