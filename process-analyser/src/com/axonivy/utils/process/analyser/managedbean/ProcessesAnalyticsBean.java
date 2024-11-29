package com.axonivy.utils.process.analyser.managedbean;

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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.process.analyser.bo.Node;
import com.axonivy.utils.process.analyser.bo.ProcessMiningData;
import com.axonivy.utils.process.analyser.bo.TimeFrame;
import com.axonivy.utils.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.utils.process.analyser.constants.ProcessAnalyticsConstants;
import com.axonivy.utils.process.analyser.enums.KpiType;
import com.axonivy.utils.process.analyser.internal.ProcessUtils;
import com.axonivy.utils.process.analyser.utils.DateUtils;
import com.axonivy.utils.process.analyser.utils.JacksonUtils;
import com.axonivy.utils.process.analyser.utils.ProcessesMonitorUtils;
import com.axonivy.utils.process.analyser.bo.CustomFieldFilter;
import com.axonivy.utils.process.analyser.service.IvyTaskOccurrenceService;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.location.IParser.ParseException;
import ch.ivyteam.ivy.process.rdm.IProcess;
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
  private Map<CustomFieldFilter, List<Object>> customFieldsByType;
  private Map<CustomFieldFilter, List<Object>> selectedCustomFilters;
  private List<String> selectedCustomFieldNames;
  private boolean isFilterDropdownVisible;

  @PostConstruct
  private void init() {
    processesMap = ProcessUtils.getProcessesWithPmv();
    applicationName = Ivy.request().getApplication().getName();
    targetHost = Ivy.html().applicationHomeLink().toAbsoluteUri().getAuthority();
    targetApplicationName = applicationName;
    setNodes(new ArrayList<>());
    processMiningDataJsonFile = ContentManagement.cms(IApplication.current()).root().child()
        .folder(ProcessAnalyticsConstants.PROCESS_ANALYSER_CMS_PATH).child()
        .file(ProcessAnalyticsConstants.DATA_CMS_PATH, ProcessAnalyticsConstants.JSON_EXTENSION);
    miningUrl = processMiningDataJsonFile.uri();
    timeIntervalFilter = TimeIntervalFilter.getDefaultFilterSet();
    customFieldsByType = new HashMap<>();
    selectedCustomFilters = new HashMap<>();
    selectedCustomFieldNames = new ArrayList<>();
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
    resetCustomFieldFilterValues();
  }

  public void onProcessSelect() {
    resetStatisticValue();
    resetCustomFieldFilterValues();
    getCaseAndTaskCustomFields();
  }

  public void onKpiTypeSelect() {
    resetStatisticValue();
  }

  private void resetStatisticValue() {
    processMiningData = null;
    nodes = new ArrayList<>();
    bpmnIframeSourceUrl = StringUtils.EMPTY;
  }

  private void resetCustomFieldFilterValues() {
    selectedCustomFieldNames = new ArrayList<>();
    customFieldsByType.clear();
    setFilterDropdownVisible(false);
  }

  public Map<CustomFieldFilter, List<Object>> getCaseAndTaskCustomFields() {
    Optional.ofNullable(getSelectedIProcessWebStartable()).ifPresent(process -> {
      selectedPid = process.pid().getParent().toString();
    });
    customFieldsByType = IvyTaskOccurrenceService.getCaseAndTaskCustomFields(selectedPid, timeIntervalFilter);
    return customFieldsByType;
  }

  public void onCustomFieldSelect() {
    customFieldsByType.forEach((key, value) -> {
      boolean isSelectedCustomField = selectedCustomFieldNames.contains(key.getCustomFieldMeta().name());
      if (isSelectedCustomField && ObjectUtils.isEmpty(selectedCustomFilters.get(key))) {
        // Initialize the number range for custom field type NUMBER
        if (CustomFieldType.NUMBER == key.getCustomFieldMeta().type()) {
          double minValue = getMinValue(key.getCustomFieldMeta().name());
          double maxValue = getMaxValue(key.getCustomFieldMeta().name());
          selectedCustomFilters.put(key, Arrays.asList(minValue, maxValue));
        } else {
          selectedCustomFilters.put(key, new ArrayList<>());
        }
      } else if (!isSelectedCustomField) {
        selectedCustomFilters.remove(key);
      }
    });
    setFilterDropdownVisible(!selectedCustomFieldNames.isEmpty());
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
    return customFieldsByType.entrySet().stream()
        .filter(entry -> entry.getKey().getCustomFieldMeta().name().equals(fieldName))
        .flatMap(entry -> entry.getValue().stream()).filter(obj -> obj instanceof Number)
        .mapToDouble(obj -> ((Number) obj).doubleValue());
  }

  public void onNumberSliderChange(CustomFieldFilter customField, double minValue, double maxValue) {
    if (CustomFieldType.NUMBER == customField.getCustomFieldMeta().type()) {
      selectedCustomFilters.put(customField, Arrays.asList(minValue, maxValue));
    }
  }

  public String getRangeDisplayForNumberType(List<Double> numberValue) {
    return Ivy.cms().co("/Dialogs/com/axonivy/utils/process/analyser/ProcessesMonitor/NumberRange",
        Arrays.asList(numberValue.get(0), numberValue.get(1)));
  }

  public void updateDataOnChangingFilter() throws ParseException {
    var parameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String from = parameterMap.get(ProcessAnalyticsConstants.FROM);
    String to = parameterMap.get(ProcessAnalyticsConstants.TO);
    timeIntervalFilter.setFrom(DateUtils.parseDateFromString(from));
    timeIntervalFilter.setTo(DateUtils.parseDateFromString(to));
    resetCustomFieldFilterValues();
    getCaseAndTaskCustomFields();
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
    bpmnIframeSourceUrl = String.format(ProcessAnalyticsConstants.PROCESS_ANALYSER_SOURCE_URL_PATTERN, applicationName,
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
            getSelectedIProcessWebStartable(), timeIntervalFilter, selectedKpiType, selectedCustomFilters);
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

  public Map<CustomFieldFilter, List<Object>> getCustomFieldsByType() {
    return customFieldsByType;
  }

  public void setCustomFieldsByType(Map<CustomFieldFilter, List<Object>> customFieldsByType) {
    this.customFieldsByType = customFieldsByType;
  }

  public Map<CustomFieldFilter, List<Object>> getSelectedCustomFilters() {
    return selectedCustomFilters;
  }

  public void setSelectedCustomFilters(Map<CustomFieldFilter, List<Object>> selectedCustomFilters) {
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
}
