package com.axonivy.utils.bpmnstatistic.managedbean;

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

import com.axonivy.utils.bpmnstatistic.bo.Node;
import com.axonivy.utils.bpmnstatistic.bo.ProcessMiningData;
import com.axonivy.utils.bpmnstatistic.bo.TimeFrame;
import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.constants.ProcessAnalyticsConstants;
import com.axonivy.utils.bpmnstatistic.enums.KpiType;
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;
import com.axonivy.utils.bpmnstatistic.utils.DateUtils;
import com.axonivy.utils.bpmnstatistic.utils.JacksonUtils;
import com.axonivy.utils.bpmnstatistic.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.location.IParser.ParseException;
import ch.ivyteam.ivy.process.rdm.IProcess;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.custom.field.CustomFieldType;
import ch.ivyteam.ivy.workflow.custom.field.ICustomField;
import ch.ivyteam.ivy.workflow.query.TaskQuery;
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
  private List<String> availableCustomFields = new ArrayList<>();
  private Map<String, List<String>> customFieldMap = new HashMap<>();
  private Map<CustomFieldType, Map<String, List<String>>> customFieldsByType = new HashMap<>();
  private Map<CustomFieldType, Map<String, String>> selectedCustomFilters = new HashMap<>();
  private List<String> selectedKeys = new ArrayList<>();
  private boolean isFilterDropdownVisible;

  @PostConstruct
  private void init() {
    processesMap = ProcessUtils.getProcessesWithPmv();
    applicationName = Ivy.request().getApplication().getName();
    targetHost = Ivy.html().applicationHomeLink().toAbsoluteUri().getAuthority();
    targetApplicationName = applicationName;
    setNodes(new ArrayList<>());
    processMiningDataJsonFile = ContentManagement.cms(IApplication.current()).root().child()
        .folder(ProcessAnalyticsConstants.PROCESS_MINING_CMS_PATH).child()
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

  public Map<CustomFieldType, Map<String, List<String>>> getCaseAndTaskCustomFields() {
    Optional.ofNullable(getSelectedIProcessWebStartable()).ifPresent(process -> {
      selectedPid = process.pid().getParent().toString();
    });
    return Sudo.get(() -> {
      List<ITask> tasks = getTasksFromSelectedProcess();
      List<ICustomField<?>> allCustomFields = getAllCustomFields(tasks);
      customFieldsByType.clear();
      for (ICustomField<?> customField : allCustomFields) {
        CustomFieldType customFieldType = customField.type();
        String customFieldName = customField.name();
        String customFieldValue = (String) customField.getOrNull();

        customFieldMap = customFieldsByType.computeIfAbsent(customFieldType, k -> new HashMap<>());
        customFieldMap.computeIfAbsent(customFieldName, k -> new ArrayList<>()).add(customFieldValue);
      }
      return customFieldsByType;
    });
  }

  private List<ITask> getTasksFromSelectedProcess() {
    return TaskQuery.create().where().requestPath().isLike(String.format("%%%s%%", selectedPid)).executor().results();
  }

  private List<ICustomField<?>> getAllCustomFields(List<ITask> tasks) {
    List<ICustomField<?>> allCustomFields = new ArrayList<>();

    for (ITask task : tasks) {
      allCustomFields.addAll(task.customFields().all());
      allCustomFields.addAll(task.getCase().customFields().all());
    }
    return allCustomFields;
  }

  public void onCustomFieldSelect() {
    selectedCustomFilters.clear();
    for (String fieldName : selectedKeys) {
      for (Map.Entry<CustomFieldType, Map<String, List<String>>> entry : customFieldsByType.entrySet()) {
          CustomFieldType fieldType = entry.getKey();
          Map<String, List<String>> fieldMap = entry.getValue();

          if (fieldMap.containsKey(fieldName)) {
              // Get the first distinct value (or default to an empty string if not needed).
              String selectedValue = fieldMap.get(fieldName).stream().distinct().findFirst().orElse("");

              // Insert selected single value per fieldName under the correct CustomFieldType
              selectedCustomFilters
                  .computeIfAbsent(fieldType, k -> new HashMap<>())
                  .put(fieldName, selectedValue);
          }
      }
  }
    setFilterDropdownVisible(!selectedCustomFilters.isEmpty());
  }

  public void onCustomFieldChange() {
    resetStatisticValue();
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
    bpmnIframeSourceUrl = String.format(ProcessAnalyticsConstants.BPMN_STATISTIC_SOURCE_URL_PATTERN, applicationName,
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
        nodes = ProcessesMonitorUtils.filterInitialStatisticByIntervalWithoutModifyingProcess(
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

  public List<String> getAvailableCustomFields() {
    return availableCustomFields;
  }

  public void setAvailableCustomFields(List<String> availableCustomFields) {
    this.availableCustomFields = availableCustomFields;
  }

  public Map<String, List<String>> getCustomFieldMap() {
    return customFieldMap;
  }

  public void setCustomFieldMap(Map<String, List<String>> customFieldMap) {
    this.customFieldMap = customFieldMap;
  }

  public Map<CustomFieldType, Map<String, List<String>>> getCustomFieldsByType() {
    return customFieldsByType;
  }

  public void setCustomFieldsByType(Map<CustomFieldType, Map<String, List<String>>> customFieldsByType) {
    this.customFieldsByType = customFieldsByType;
  }

  public Map<CustomFieldType, Map<String, String>> getSelectedCustomFilters() {
    return selectedCustomFilters;
  }

  public void setSelectedCustomFilters(Map<CustomFieldType, Map<String, String>> selectedCustomFilters) {
    this.selectedCustomFilters = selectedCustomFilters;
  }

  public boolean isFilterDropdownVisible() {
    return isFilterDropdownVisible;
  }

  public void setFilterDropdownVisible(boolean isFilterDropdownVisible) {
    this.isFilterDropdownVisible = isFilterDropdownVisible;
  }

  public List<String> getSelectedKeys() {
    return selectedKeys;
  }

  public void setSelectedKeys(List<String> selectedKeys) {
    this.selectedKeys = selectedKeys;
  }
}
