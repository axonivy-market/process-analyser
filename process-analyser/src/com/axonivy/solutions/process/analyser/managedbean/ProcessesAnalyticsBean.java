package com.axonivy.solutions.process.analyser.managedbean;

import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.ANALYSIS_EXCEL_FILE_PATTERN;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.DATA_CMS_PATH;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.EN_CMS_LOCALE;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.FROM;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.JSON_EXTENSION;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.MULTIPLE_UNDERSCORES_REGEX;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.PROCESS_ANALYSER_CMS_PATH;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.SPACE_DASH_REGEX;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.TO;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.UNDERSCORE;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.UPDATE_IFRAME_SOURCE_METHOD_CALL;
import static com.axonivy.solutions.process.analyser.core.constants.CoreConstants.HYPHEN_SIGN;
import static com.axonivy.solutions.process.analyser.core.enums.StartElementType.StartEventElement;
import static com.axonivy.solutions.process.analyser.core.enums.StartElementType.StartSignalEventElement;
import static com.axonivy.solutions.process.analyser.core.enums.StartElementType.WebServiceProcessStartElement;

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
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.primefaces.PF;

import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.ProcessAnalyser;
import com.axonivy.solutions.process.analyser.bo.ProcessMiningData;
import com.axonivy.solutions.process.analyser.bo.ProcessViewerConfig;
import com.axonivy.solutions.process.analyser.bo.TimeFrame;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.constants.ProcessAnalyticViewComponentId;
import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.bo.StartElement;
import com.axonivy.solutions.process.analyser.core.constants.CoreConstants;
import com.axonivy.solutions.process.analyser.core.enums.StartElementType;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.service.IvyTaskOccurrenceService;
import com.axonivy.solutions.process.analyser.utils.DateUtils;
import com.axonivy.solutions.process.analyser.utils.FacesContexts;
import com.axonivy.solutions.process.analyser.utils.JacksonUtils;
import com.axonivy.solutions.process.analyser.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.ILibrary;
import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.custom.field.CustomFieldType;

@ManagedBean
@ViewScoped
public class ProcessesAnalyticsBean {
  private static final String SUB_PROCESS_CALL_PID_PARAM_NAME = "subProcessCallPid";
  private Map<String, List<Process>> processesMap = new HashMap<>();
  private ProcessAnalyser selectedProcessAnalyser;
  private String selectedModule;
  private String selectedPMVVersion;
  private IProcessModelVersion selectedPMV;
  private KpiType selectedKpiType;
  private List<Node> nodes;
  private List<Node> analyzedNode;
  private List<Node> filteredNodes;
  private TimeIntervalFilter timeIntervalFilter;
  private ProcessMiningData processMiningData;
  private String selectedPid;
  private String miningUrl;
  private ContentObject processMiningDataJsonFile;
  private List<CustomFieldFilter> customFieldsByType;
  private List<CustomFieldFilter> selectedCustomFilters;
  private List<String> selectedCustomFieldNames;
  private boolean isFilterDropdownVisible;
  private boolean isIncludingRunningCases;
  private boolean isMergeProcessStarts;
  private double minValue;
  private double maxValue;
  private List<SelectItem> kpiTypes;
  private MasterDataBean masterDataBean;
  private ProcessViewerBean viewerBean;
  private ColorPickerBean colorPickerBean;
  private boolean isWidgetMode;

  @PostConstruct
  private void init() {
    masterDataBean = FacesContexts.evaluateValueExpression("#{masterDataBean}", MasterDataBean.class);
    viewerBean = FacesContexts.evaluateValueExpression("#{processViewerBean}", ProcessViewerBean.class);
    colorPickerBean = FacesContexts.evaluateValueExpression("#{colorPickerBean}", ColorPickerBean.class);
    var isWidgetModeValue = FacesContexts.evaluateValueExpression("#{data.isWidgetMode}", Boolean.class);
    isWidgetMode = BooleanUtils.isTrue(isWidgetModeValue);
    processesMap = masterDataBean.getProcessesMap();
    nodes = new ArrayList<>();
    processMiningDataJsonFile = ContentManagement.cms(IApplication.current()).root().child()
        .folder(PROCESS_ANALYSER_CMS_PATH).child().file(DATA_CMS_PATH, JSON_EXTENSION);
    miningUrl = processMiningDataJsonFile.uri();
    timeIntervalFilter = TimeIntervalFilter.getDefaultFilterSet();
    customFieldsByType = new ArrayList<>();
    selectedCustomFilters = new ArrayList<>();
    selectedCustomFieldNames = new ArrayList<>();
    initKpiTypes();
    initSelectedValueFromUserProperty();
  }

  private void initSelectedValueFromUserProperty() {
    ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
    // Early escapes if not in widget mode
    if (!isWidgetMode) {
      return;
    }
    selectedModule = persistedConfig.getWidgetSelectedModule();
    isMergeProcessStarts = BooleanUtils.isTrue(persistedConfig.getWidgetMergedProcessStart());
    isIncludingRunningCases = BooleanUtils.isTrue(persistedConfig.getWidgetIncludeRunningCase());
    String selectedKpiTypeName = persistedConfig.getWidgetSelectedKpi();
    String selectedProcessAnalyzerId = persistedConfig.getWidgetSelectedProcessAnalyzer();
    if (StringUtils.isNoneBlank(selectedModule, selectedProcessAnalyzerId)) {
      selectedProcessAnalyser = initSelectedProcessAnalyser(selectedProcessAnalyzerId);
    }
    if (StringUtils.isNotBlank(selectedKpiTypeName)) {
      selectedKpiType = KpiType.valueOf(selectedKpiTypeName);
    }
    colorPickerBean.initBean(selectedKpiType, isWidgetMode);
    updateDiagramAndStatistic();
  }

  private ProcessAnalyser initSelectedProcessAnalyser(String selectedProcessAnalyzerId) {
    ProcessAnalyser persistedProcessAnalyser = new ProcessAnalyser();
    String[] parts = selectedProcessAnalyzerId.split(HYPHEN_SIGN, 2);
    if (parts.length >= 1) {
      String selectedProcessId = parts[0];
      var selectedProcess = processesMap.get(selectedModule).stream()
          .filter(process -> Strings.CS.equals(process.getId(), selectedProcessId)).findAny().orElse(null);
      if (selectedProcess == null) {
        return null;
      }
      persistedProcessAnalyser.setProcess(selectedProcess);
      String selectedStartPid = parts.length == 2 ? parts[1] : StringUtils.EMPTY;
      if (StringUtils.isBlank(selectedStartPid)) {
        return persistedProcessAnalyser;
      }
      var selectedProcessStart = selectedProcess.getStartElements().stream()
          .filter(start -> Strings.CS.equals(start.getPid(), selectedStartPid)).findAny().orElse(null);
      persistedProcessAnalyser.setStartElement(selectedProcessStart);
    }
    return persistedProcessAnalyser;
  }

  public void updateDataTable() {
    String subProcessCallPid = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
        .get(SUB_PROCESS_CALL_PID_PARAM_NAME);
    if (StringUtils.isNotBlank(subProcessCallPid)) {
      updateDataTableWithNodesPrefix(subProcessCallPid);
      renderNodesForKPIType();
    }
  }

  private void updateDataTableWithNodesPrefix(String prefix) {
    nodes = analyzedNode.stream().filter(node -> node.getId().startsWith(prefix)).collect(Collectors.toList());
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

  /**
   * Returns a list of selectable process start options for the current module.
   *
   * - If no module is selected, returns an empty list.
   * - In "merge process starts" mode, each process is a single {@link SelectItem}.
   * - Otherwise, processes with one start element create a single item,
   *   and processes with multiple start elements create a {@link SelectItemGroup}.
   *
   * @return list of available process starts for the selected module
   */
  public List<SelectItem> getAvailableProcessStarts() {
    if (StringUtils.isBlank(selectedModule)) {
      return new ArrayList<>();
    }
    List<SelectItem> processStartsSelection = new ArrayList<>();
    processesMap.get(selectedModule).stream().filter(process -> CollectionUtils.isNotEmpty(process.getStartElements()))
        .forEach(process -> {
          if (isMergeProcessStarts) {
            addMergeProcessStart(process, processStartsSelection);
          } else {
            handleProcessStarts(process, processStartsSelection);
          }
        });

    return processStartsSelection;
  }

  private void addMergeProcessStart(Process process, List<SelectItem> processStartsSelection) {
    var processItem = new SelectItem(new ProcessAnalyser(process), process.getName(), process.getName());
    processStartsSelection.add(processItem);
  }

  private void handleProcessStarts(Process process, List<SelectItem> processStartsSelection) {
    if (process.getStartElements().size() == 1) {
      addSingleStartElement(process, processStartsSelection);
    } else {
      addMultipleStartElements(process, processStartsSelection);
    }
  }

  private void addSingleStartElement(Process process, List<SelectItem> processStartsSelection) {
    var startElement = process.getStartElements().getFirst();
    var item = createNewProcessItemForDropdown(process, startElement);

    var processNameAndStartElement = process.getName().concat(CoreConstants.SLASH).concat(item.getLabel());
    item.setLabel(processNameAndStartElement);

    processStartsSelection.add(item);
  }

  private void addMultipleStartElements(Process process, List<SelectItem> processStartsSelection) {
    var group = new SelectItemGroup(process.getName());
    group.setValue(new ProcessAnalyser(process));

    SelectItem[] startElementsSelection = process.getStartElements().stream()
        .map(startElement -> createNewProcessItemForDropdown(process, startElement)).toArray(SelectItem[]::new);

    group.setSelectItems(startElementsSelection);
    processStartsSelection.add(group);
  }

  private SelectItem createNewProcessItemForDropdown(Process process, StartElement startElement) {
    var processStartElement = new ProcessAnalyser(process, startElement);
    String displayName = getStartElementDisplayName(startElement);
    var description = process.getName().concat(CoreConstants.SLASH).concat(displayName);
    return new SelectItem(processStartElement, displayName, description);
  }

  private String getStartElementDisplayName(StartElement start) {
    final var enumCmsURI = "/Enums/StartElementType/%s/name";
    String cmsUrl = switch (start.getType()) {
    case StartElement -> enumCmsURI.formatted(StartElementType.StartElement.name());
    case StartEventElement -> enumCmsURI.formatted(StartEventElement.name());
    case StartSignalEventElement -> enumCmsURI.formatted(StartSignalEventElement.name());
    case WebServiceProcessStartElement -> enumCmsURI.formatted(WebServiceProcessStartElement.name());
    default -> start.getName();
    };
    return Ivy.cms().co(cmsUrl, List.of(start.getName()));
  }

  public void prepareForExportingJPEG(boolean isResetView) {
    if (isResetView) {
      viewerBean.resetViewerSelection();
      PF.current().ajax().update(ProcessAnalyticViewComponentId.getDiagramAndStatisticComponentIds());
    }
  }

  public Set<String> getAvailableModules() {
    return processesMap.keySet();
  }
  
  public List<IProcessModelVersion> getAvailabelPMV() {
    if (StringUtils.isEmpty(this.selectedModule)) {
      return List.of();
    }
    return IApplication.current().getLibraries().stream()
        .filter(library -> library.getProcessModelVersion().getVersionName().contains(this.selectedModule))
        .map(ILibrary::getProcessModelVersion).toList();
  }

  private boolean isDiagramAndStatisticRenderable() {
    return ObjectUtils.allNotNull(selectedProcessAnalyser, selectedKpiType);
  }

  public void onModuleSelect() {
    selectedProcessAnalyser = null;
    if (isWidgetMode) {
      ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
      persistedConfig.setWidgetSelectedModule(selectedModule);
      ProcessesMonitorUtils.updateUserProperty(persistedConfig);
      PF.current().ajax().update(ProcessAnalyticViewComponentId.WIDGET_PROCESS_SELECTION_GROUP);
      PF.current().ajax().update(ProcessAnalyticViewComponentId.PMV_GROUP);
      return;
    }
    PF.current().ajax().update(ProcessAnalyticViewComponentId.PROCESS_SELECTION_GROUP);
    PF.current().ajax().update(ProcessAnalyticViewComponentId.PMV_GROUP);
    resetStatisticValue();
  }
  
  public void onPmvSelect() {
    selectedPMV = IApplication.current().findProcessModelVersion(this.selectedPMVVersion);
    refreshAnalyserReportToView();
//    resetStatisticValue();
  }

  public void onProcessSelect() {
    if (isWidgetMode) {
      String widgetSelectedProcessAnalyzer = getWidgetSelectedProcessAnalyzerKey();
      ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
      persistedConfig.setWidgetSelectedProcessAnalyzer(widgetSelectedProcessAnalyzer);
      ProcessesMonitorUtils.updateUserProperty(persistedConfig);
      return;
    }
    resetStatisticValue();
    if (selectedProcessAnalyser != null) {
      refreshAnalyserReportToView();
    }
  }

  private String getWidgetSelectedProcessAnalyzerKey() {
    String selectedProcessId = Optional.ofNullable(selectedProcessAnalyser.getProcess()).map(Process::getId)
        .orElse(StringUtils.EMPTY);
    String selectedStartId = Optional.ofNullable(selectedProcessAnalyser.getStartElement()).map(StartElement::getPid)
        .orElse(StringUtils.EMPTY);
    String widgetSelectedProcessAnalyzer = isMergeProcessStarts ? selectedProcessId
        : String.join(HYPHEN_SIGN, selectedProcessId, selectedStartId);
    return widgetSelectedProcessAnalyzer;
  }

  public void onKpiTypeSelect() {
    if (isWidgetMode) {
      ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
      persistedConfig.setWidgetSelectedKpi(selectedKpiType.name());
      ProcessesMonitorUtils.updateUserProperty(persistedConfig);
      return;
    }
    colorPickerBean.updateColorByKpiType(selectedKpiType);
    refreshAnalyserReportToView();
  }

  private void resetStatisticValue() {
    resetCustomFieldFilterValues();
    processMiningData = null;
    nodes = new ArrayList<>();
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
    if (selectedProcessAnalyser == null || selectedProcessAnalyser.getProcess() == null) {
      return new ArrayList<>();
    }
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
    refreshAnalyserReportToView();
  }

  private void updateCustomFilterPanel() {
    List<String> groupIdsToUpdate = List.of(ProcessAnalyticViewComponentId.CUSTOM_FILTER_GROUP,
        ProcessAnalyticViewComponentId.CUSTOM_FILTER_OPTIONS_GROUP);
    PF.current().ajax().update(groupIdsToUpdate);
  }

  public void onCustomfieldUnselect() {
    onCustomFieldSelect();
    updateCustomFilterPanel();
    refreshAnalyserReportToView();
  }

  public double getMinValue(String fieldName) {
    if (getDoubleValueFromCustomNumberField(fieldName).count() > 1) {
      return getDoubleValueFromCustomNumberField(fieldName).map(value -> Math.floor(value * 100) / 100).min().orElse(0);
    }
    return 0;
  }

  public double getMaxValue(String fieldName) {
    return getDoubleValueFromCustomNumberField(fieldName).map(value -> Math.ceil(value * 100) / 100).max().orElse(0);
  }

  private DoubleStream getDoubleValueFromCustomNumberField(String fieldName) {
    return customFieldsByType.stream().filter(entry -> entry.getCustomFieldMeta().name().equals(fieldName))
        .flatMap(entry -> entry.getAvailableCustomFieldValues().stream()).filter(Number.class::isInstance)
        .mapToDouble(value -> Number.class.cast(value).doubleValue());
  }

  public String getRangeDisplayForNumberType(List<Double> numberValue) {
    return Ivy.cms().co("/Dialogs/com/axonivy/solutions/process/analyser/ProcessesMonitor/NumberRange",
        Arrays.asList(numberValue.get(0), numberValue.get(1)));
  }

  public void refreshAnalyserReportToView() {
    updateDiagramAndStatistic();
    updateCustomFilterPanel();
    renderNodesForKPIType();
  }

  public void onChangeIncludingRunningCases() {
    if (isWidgetMode) {
      ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
      persistedConfig.setWidgetIncludeRunningCase(isIncludingRunningCases);
      ProcessesMonitorUtils.updateUserProperty(persistedConfig);
    }
    updateDiagramAndStatistic();
  }

  public void onColorChange() {
    colorPickerBean.onColorChange();
    if (!isWidgetMode) {      
      refreshAnalyserReportToView();
    }
  }

  public void onChangeMergeProcessStarts() {
    if (isWidgetMode) {
      ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
      persistedConfig.setWidgetMergedProcessStart(isMergeProcessStarts);
      ProcessesMonitorUtils.updateUserProperty(persistedConfig);
    }
    updateDiagramAndStatistic();
  }

  public void onColorModeChange() {
    colorPickerBean.onColorModeChange();
    if (!isWidgetMode) {
      refreshAnalyserReportToView();
    }
  }

  public void updateDataOnChangingFilter() {
    var parameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String from = parameterMap.get(FROM);
    String to = parameterMap.get(TO);
    timeIntervalFilter.setFrom(DateUtils.parseDateFromString(from));
    timeIntervalFilter.setTo(DateUtils.parseDateFromString(to));
    if (!isWidgetMode) {
      resetStatisticValue();
      refreshAnalyserReportToView();
    }
  }

  public void updateDiagramAndStatistic() {
    if (isDiagramAndStatisticRenderable()) {
      viewerBean.init(selectedProcessAnalyser);
      loadNodes();
      updateProcessMiningDataJson();
      renderNodesForKPIType();
      PF.current().executeScript(UPDATE_IFRAME_SOURCE_METHOD_CALL);
      PF.current().ajax().update(ProcessAnalyticViewComponentId.getDiagramAndStatisticComponentIds());
    }
  }

  private void loadNodes() {
    analyzedNode = new ArrayList<>();
    selectedPid = selectedProcessAnalyser.getProcess().getId();
    initializingProcessMiningData();
    if (haveMandatoryFieldsBeenFilled()) {
      List<ICase> cases = new ArrayList<>();
      boolean shouldIncludeRunningCasesByKpi = isIncludingRunningCases && !selectedKpiType.isDescendantOf(KpiType.DURATION);
      if (isMergeProcessStarts) {
        List<Long> taskStartIds = selectedProcessAnalyser.getProcess().getStartElements().stream()
            .map(StartElement::getTaskStartId).toList();
        for (Long taskStartId : taskStartIds) {
          List<ICase> subCases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(taskStartId, timeIntervalFilter,
              selectedCustomFilters, shouldIncludeRunningCasesByKpi, this.selectedPMV);
          if (CollectionUtils.isNotEmpty(subCases)) {
            cases.addAll(subCases);
          }
        }
      } else {
        Long taskStartId = selectedProcessAnalyser.getStartElement().getTaskStartId();
        cases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(taskStartId, timeIntervalFilter,
            selectedCustomFilters, shouldIncludeRunningCasesByKpi, this.selectedPMV);
      }
      if (CollectionUtils.isNotEmpty(cases)) {
        var tasks = cases.stream().flatMap(ivyCase -> ivyCase.tasks().all().stream()).toList();
        customFieldsByType = IvyTaskOccurrenceService.getCaseAndTaskCustomFields(tasks, customFieldsByType);
        analyzedNode = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(selectedProcessAnalyser,
            selectedKpiType, cases);
        processMiningData.setNodes(analyzedNode);
        processMiningData.setNumberOfInstances(cases.size());
      }
    }
    updateDataTableWithNodesPrefix(ProcessUtils.getProcessPidFromElement(selectedPid));
  }

  private boolean haveMandatoryFieldsBeenFilled() {
    return StringUtils.isNoneBlank(selectedModule) && ObjectUtils.allNotNull(selectedKpiType, selectedProcessAnalyser);
  }

  private void updateProcessMiningDataJson() {
    String jsonString = JacksonUtils.convertObjectToJSONString(processMiningData);
    processMiningDataJsonFile.value().get(EN_CMS_LOCALE).write().string(jsonString);
  }

  private void initializingProcessMiningData() {
    Optional.ofNullable(selectedProcessAnalyser.getProcess()).ifPresent(selectedProcess -> {
      List<Node> nodes = new ArrayList<>();
      nodes.add(new Node());
      processMiningData = new ProcessMiningData();
      processMiningData.setProcessId(selectedProcess.getId());
      processMiningData.setProcessName(selectedProcess.getName());
      processMiningData.setKpiType(selectedKpiType);
      processMiningData.setTimeFrame(new TimeFrame(timeIntervalFilter.getFrom(), timeIntervalFilter.getTo()));
      processMiningData.setColors(colorPickerBean.getColorSegments());
      processMiningData.setTextColors(colorPickerBean.getTextColors());
      processMiningData.setNodes(nodes);
    });
  }

  public String generateNameOfExcelFile() {
    String formattedKpiTypeName = selectedKpiType.getCmsName().replaceAll(SPACE_DASH_REGEX, UNDERSCORE)
        .replaceAll(MULTIPLE_UNDERSCORES_REGEX, UNDERSCORE);
    var startName = Optional.ofNullable(selectedProcessAnalyser)
        .map(analyser -> isMergeProcessStarts
            ? Optional.ofNullable(analyser.getProcess()).map(Process::getName).orElse(StringUtils.EMPTY)
            : Optional.ofNullable(analyser.getStartElement()).map(StartElement::getName).orElse(StringUtils.EMPTY))
        .orElse(StringUtils.EMPTY);
    return String.format(ANALYSIS_EXCEL_FILE_PATTERN, formattedKpiTypeName, startName);
  }

  public void renderNodesForKPIType() {
    filteredNodes = new ArrayList<>(nodes);
    if (this.selectedKpiType != null && this.selectedKpiType.isDescendantOf(KpiType.DURATION)) {
      List<String> avaibleTaskIds = filteredNodes.stream().filter(node -> node.getType() == NodeType.ARROW)
          .map(node -> node.getSourceNodeId()).toList();

      filteredNodes = filteredNodes.stream()
          .filter(node -> node.getType() != NodeType.ARROW && avaibleTaskIds.contains(node.getId()))
          .collect(Collectors.toList());
    }
  }

  public boolean isMedianDurationColumnVisible() {
    return ProcessesMonitorUtils.isDuration(selectedKpiType);
  }

  public String getCalulatedCellColor(Double value) {
    return colorPickerBean.getCalulatedCellColor(value);
  }

  public String getAccessibleTextColor(Double value) {
    return colorPickerBean.getAccessibleTextColor(value);
  }

  public ProcessAnalyser getSelectedProcessAnalyser() {
    return selectedProcessAnalyser;
  }

  public void setSelectedProcessAnalyser(ProcessAnalyser selectedProcessAnalyser) {
    this.selectedProcessAnalyser = selectedProcessAnalyser;
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
    return StringUtils.isBlank(selectedModule) || selectedProcessAnalyser == null || selectedKpiType == null;
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

  public Boolean getIsWidgetMode() {
    return isWidgetMode;
  }

  public void setIsWidgetMode(Boolean isWidgetMode) {
    this.isWidgetMode = isWidgetMode;
  }

  public boolean isMergeProcessStarts() {
    return isMergeProcessStarts;
  }

  public String getSelectedPMVVersion() {
    return selectedPMVVersion;
  }

  public void setSelectedPMVVersion(String selectedPMVVersion) {
    this.selectedPMVVersion = selectedPMVVersion;
  }

  public void setMergeProcessStarts(boolean isMergeProcessStarts) {
    this.isMergeProcessStarts = isMergeProcessStarts;
    resetProcessSelection();
  }

  private void resetProcessSelection() {
    selectedProcessAnalyser = null;
    String componentIdToUpdate = isWidgetMode ? ProcessAnalyticViewComponentId.WIDGET_PROCESS_SELECTION_GROUP
        : ProcessAnalyticViewComponentId.PROCESS_SELECTION_GROUP;
    PF.current().ajax().update(componentIdToUpdate);
  }
}
