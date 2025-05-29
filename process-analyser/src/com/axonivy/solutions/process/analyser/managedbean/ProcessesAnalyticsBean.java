package com.axonivy.solutions.process.analyser.managedbean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PF;

import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.ProcessMiningData;
import com.axonivy.solutions.process.analyser.bo.TimeFrame;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.constants.ProcessAnalyticViewComponentId;
import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.utils.DateUtils;
import com.axonivy.solutions.process.analyser.utils.JacksonUtils;
import com.axonivy.solutions.process.analyser.utils.ProcessesMonitorUtils;
import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.service.IvyTaskOccurrenceService;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.location.IParser.ParseException;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.custom.field.CustomFieldType;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@ManagedBean
@ViewScoped
public class ProcessesAnalyticsBean {
  private Map<String, List<IProcessWebStartable>> processesMap = new HashMap<>();
  private String selectedProcess;
  private String selectedModule;
  private KpiType selectedKpiType;
  private List<Node> nodes;
  private TimeIntervalFilter timeIntervalFilter;
  private ProcessMiningData processMiningData;
  private String selectedPid;
  private String miningUrl;
  private ContentObject processMiningDataJsonFile;
  private String bpmnIframeSourceUrl;
  private List<CustomFieldFilter> customFieldsByType;
  private List<CustomFieldFilter> selectedCustomFilters;
  private List<String> selectedCustomFieldNames;
  private boolean isFilterDropdownVisible;
  private double minValue;
  private double maxValue;
  private List<SelectItem> kpiTypes;

  @PostConstruct
  private void init() {
    processesMap = ProcessUtils.getProcessesWithPmv();
    setNodes(new ArrayList<>());
    processMiningDataJsonFile = ContentManagement.cms(IApplication.current()).root().child()
        .folder(ProcessAnalyticsConstants.PROCESS_ANALYSER_CMS_PATH).child()
        .file(ProcessAnalyticsConstants.DATA_CMS_PATH, ProcessAnalyticsConstants.JSON_EXTENSION);
    miningUrl = processMiningDataJsonFile.uri();
    timeIntervalFilter = TimeIntervalFilter.getDefaultFilterSet();
    customFieldsByType = new ArrayList<>();
    selectedCustomFilters = new ArrayList<>();
    selectedCustomFieldNames = new ArrayList<>();
    initKpiTypes();
  }

  private void initKpiTypes() {
    kpiTypes = new ArrayList<>();

    for (KpiType type : KpiType.getTopLevelOptions()) {
      kpiTypes.add(createSelectItem(type));
    }
  }

  private SelectItem createSelectItem(KpiType type) {
    List<KpiType> subOptions = type.getSubOptions();
    if (subOptions.isEmpty()) {
      return new SelectItem(type, type.getCmsName());
    }
    SelectItemGroup group = new SelectItemGroup(type.getCmsName());
    group.setValue(type);
    group.setSelectItems(subOptions.stream().map(this::createSelectItem).toArray(SelectItem[]::new));
    return group;
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

  private boolean isDiagramAndStatisticRenderable() {
    return ObjectUtils.allNotNull(selectedProcess, selectedKpiType);
  }

  public void onModuleSelect() {
    selectedProcess = null;
    PF.current().ajax().update(ProcessAnalyticViewComponentId.PROCESS_DROPDOWN);
    if (StringUtils.isNotBlank(bpmnIframeSourceUrl)) {
      resetStatisticValue();
    }
  }

  public void onProcessSelect() {
    resetStatisticValue();
    if (StringUtils.isNotBlank(selectedProcess)) {
      updateDiagramAndStatistic();
      getCaseAndTaskCustomFields();
    }
  }

  public void onKpiTypeSelect() {
    updateDiagramAndStatistic();
  }

  private void resetStatisticValue() {
    resetCustomFieldFilterValues();
    processMiningData = null;
    nodes = new ArrayList<>();
    bpmnIframeSourceUrl = StringUtils.EMPTY;
    PF.current().ajax().update(ProcessAnalyticViewComponentId.getDiagramAndStatisticComponentIds());
  }

  private void resetCustomFieldFilterValues() {
    selectedCustomFieldNames = new ArrayList<>();
    selectedCustomFilters = new ArrayList<>();
    customFieldsByType = new ArrayList<>();
    setFilterDropdownVisible(false);
    updateCustomFilterPanel();
  }

  public List<CustomFieldFilter> getCaseAndTaskCustomFields() {
    Optional.ofNullable(getSelectedIProcessWebStartable()).ifPresent(process -> {
      selectedPid = process.pid().getParent().toString();
    });
    customFieldsByType = IvyTaskOccurrenceService.getCaseAndTaskCustomFields(selectedPid, timeIntervalFilter);
    return customFieldsByType;
  }

  public void onCustomFieldSelect() {
    customFieldsByType.forEach(customField -> {
      boolean isSelectedCustomField = selectedCustomFieldNames.contains(customField.getCustomFieldMeta().name());
      if (isSelectedCustomField && !selectedCustomFilters.contains(customField)) {
        // Initialize the number range for custom field type NUMBER
        if (CustomFieldType.NUMBER == customField.getCustomFieldMeta().type()) {
          minValue = getMinValue(customField.getCustomFieldMeta().name());
          maxValue = getMaxValue(customField.getCustomFieldMeta().name());
          customField.setCustomFieldValues(Arrays.asList(minValue, maxValue));

        }
        selectedCustomFilters.add(customField);
      } else if (!isSelectedCustomField) {
        customField.setCustomFieldValues(new ArrayList<>());
        customField.setTimestampCustomFieldValues(new ArrayList<>());
        selectedCustomFilters.removeIf(selectedFilter -> selectedFilter.getCustomFieldMeta().name()
            .equals(customField.getCustomFieldMeta().name()));
      }
    });
    setFilterDropdownVisible(!selectedCustomFieldNames.isEmpty());
    updateCustomFilterPanel();
  }

  private void updateCustomFilterPanel() {
    List<String> groupIdsToUpdate = List.of(ProcessAnalyticViewComponentId.CUSTOM_FILTER_GROUP,
        ProcessAnalyticViewComponentId.CUSTOM_FILTER_OPTIONS_GROUP);
    PF.current().ajax().update(groupIdsToUpdate);
  }

  public void onCustomfieldUnselect() {
    onCustomFieldSelect();
    updateDiagramAndStatistic();
    updateCustomFilterPanel();
  }

  public double getMinValue(String fieldName) {
    if (getNumberTypeValue(fieldName).count() > 1) {
      return getNumberTypeValue(fieldName).map(value -> Math.floor(value * 100) / 100).min().orElse(0);
    }
    return 0;
  }

  public double getMaxValue(String fieldName) {
    return getNumberTypeValue(fieldName).map(value -> Math.ceil(value * 100) / 100).max().orElse(0);
  }

  private DoubleStream getNumberTypeValue(String fieldName) {
    return customFieldsByType.stream().filter(entry -> entry.getCustomFieldMeta().name().equals(fieldName))
        .flatMap(entry -> entry.getAvailableCustomFieldValues().stream()).filter(value -> value instanceof Number)
        .mapToDouble(value -> ((Number) value).doubleValue());
  }

  public String getRangeDisplayForNumberType(List<Double> numberValue) {
    return Ivy.cms().co("/Dialogs/com/axonivy/solutions/process/analyser/ProcessesMonitor/NumberRange",
        Arrays.asList(numberValue.get(0), numberValue.get(1)));
  }

  public void updateDataOnChangingFilter() throws ParseException {
    var parameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String from = parameterMap.get(ProcessAnalyticsConstants.FROM);
    String to = parameterMap.get(ProcessAnalyticsConstants.TO);
    timeIntervalFilter.setFrom(DateUtils.parseDateFromString(from));
    timeIntervalFilter.setTo(DateUtils.parseDateFromString(to));
    resetStatisticValue();
    getCaseAndTaskCustomFields();
    updateDiagramAndStatistic();
  }

  public void updateDiagramAndStatistic() {
    if (isDiagramAndStatisticRenderable()) {
      loadNodes();
      updateProcessMiningDataJson();
      updateBpmnIframeSourceUrl();
      PF.current().executeScript(ProcessAnalyticsConstants.UPDATE_IFRAME_SOURCE_METHOD_CALL);
      PF.current().ajax().update(ProcessAnalyticViewComponentId.getDiagramAndStatisticComponentIds());
    }
  }

  private IProcessWebStartable getSelectedIProcessWebStartable() {
    return CollectionUtils.emptyIfNull(processesMap.get(selectedModule)).stream()
        .filter(process -> process.getDisplayName().equalsIgnoreCase(selectedProcess)).findAny().orElse(null);
  }

  private void updateBpmnIframeSourceUrl() {
    bpmnIframeSourceUrl = ProcessUtils.buildBpmnIFrameSourceUrl(getSelectedIProcessWebStartable().getId(), selectedModule);
  }

  private void loadNodes() {
    List<Node> analyzedNode = new ArrayList<>();
    var process = getSelectedIProcessWebStartable();
    if (StringUtils.isNoneBlank(selectedProcess, selectedModule) && ObjectUtils.allNotNull(selectedKpiType, process)) {
      selectedPid = process.pid().toString();
      Long taskStartId = ProcessUtils.getTaskStartIdFromPID(selectedPid);
      List<ICase> cases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(taskStartId,
          timeIntervalFilter, selectedCustomFilters);
      if (CollectionUtils.isNotEmpty(cases)) {
        processMiningData = new ProcessMiningData();
        processMiningData.setProcessId(process.pid().getParent().toString());
        processMiningData.setProcessName(selectedProcess);
        processMiningData.setKpiType(selectedKpiType);
        TimeFrame timeFrame = new TimeFrame(timeIntervalFilter.getFrom(), timeIntervalFilter.getTo());
        processMiningData.setTimeFrame(timeFrame);
        analyzedNode = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(process, selectedKpiType, cases);
        processMiningData.setNodes(analyzedNode);
        processMiningData.setNumberOfInstances(cases.size());
      }
    }
    nodes = analyzedNode;
  }

  private void updateProcessMiningDataJson() {
    String jsonString = JacksonUtils.convertObjectToJSONString(processMiningData);
    processMiningDataJsonFile.value().get(ProcessAnalyticsConstants.EN_CMS_LOCALE).write().string(jsonString);
  }

  public String generateNameOfExcelFile() {
    String formattedKpiTypeName = selectedKpiType.getCmsName()
        .replaceAll(ProcessAnalyticsConstants.SPACE_DASH_REGEX, ProcessAnalyticsConstants.UNDERSCORE)
        .replaceAll(ProcessAnalyticsConstants.MULTIPLE_UNDERSCORES_REGEX, ProcessAnalyticsConstants.UNDERSCORE);
    return StringUtils.isNotBlank(selectedProcess)
        ? String.format(ProcessAnalyticsConstants.ANALYSIS_EXCEL_FILE_PATTERN, formattedKpiTypeName, selectedProcess)
        : StringUtils.EMPTY;
  }

  public boolean isMedianDurationColumnVisible() {
    return ProcessesMonitorUtils.isDuration(selectedKpiType);
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

  public List<CustomFieldFilter> getCustomFieldsByType() {
    return customFieldsByType;
  }

  public void setCustomFieldsByType(List<CustomFieldFilter> customFieldsByType) {
    this.customFieldsByType = customFieldsByType;
  }

  public List<CustomFieldFilter> getSelectedCustomFilters() {
    return selectedCustomFilters;
  }

  public void setSelectedCustomFilters(List<CustomFieldFilter> selectedCustomFilters) {
    this.selectedCustomFilters = selectedCustomFilters;
  }

  public boolean isFilterDropdownVisible() {
    return isFilterDropdownVisible;
  }

  public void setFilterDropdownVisible(boolean isFilterDropdownVisible) {
    this.isFilterDropdownVisible = isFilterDropdownVisible;
  }

  public boolean isStringOrTextCustomFieldType(CustomFieldType customFieldType) {
    return CustomFieldType.STRING == customFieldType || CustomFieldType.TEXT == customFieldType;
  }

  public List<String> getSelectedCustomFieldNames() {
    return selectedCustomFieldNames;
  }

  public void setSelectedCustomFieldNames(List<String> selectedCustomFieldNames) {
    this.selectedCustomFieldNames = selectedCustomFieldNames;
  }

  public TimeIntervalFilter getTimeIntervalFilter() {
    return timeIntervalFilter;
  }

  public void setTimeIntervalFilter(TimeIntervalFilter timeIntervalFilter) {
    this.timeIntervalFilter = timeIntervalFilter;
  }

  public double getMinValue() {
    return minValue;
  }

  public void setMinValue(double minValue) {
    this.minValue = minValue;
  }

  public double getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(double maxValue) {
    this.maxValue = maxValue;
  }

  public List<SelectItem> getKpiTypes() {
    return kpiTypes;
  }
}