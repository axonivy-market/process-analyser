package com.axonivy.solutions.process.analyser.managedbean;

import static com.axonivy.solutions.process.analyser.core.enums.StartElementType.StartEventElement;
import static com.axonivy.solutions.process.analyser.core.enums.StartElementType.StartSignalEventElement;
import static com.axonivy.solutions.process.analyser.core.enums.StartElementType.WebServiceProcessStartElement;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.COLOR_MAP_HEATMAP;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.COLOR_MAP_COLOR_CHOOSER;

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

import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.ProcessAnalyser;
import com.axonivy.solutions.process.analyser.bo.ProcessMiningData;
import com.axonivy.solutions.process.analyser.bo.TimeFrame;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.constants.ProcessAnalyticViewComponentId;
import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.bo.StartElement;
import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;
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
  private double minValue;
  private double maxValue;
  private List<SelectItem> kpiTypes;
  private MasterDataBean masterDataBean;
  private ProcessViewerBean viewerBean;
  private ColorPickerBean colorPickerBean;
  private String selectedColorMode;
  
  private List<String> availableColorMaps = Arrays.asList(COLOR_MAP_HEATMAP, COLOR_MAP_COLOR_CHOOSER);

  @PostConstruct
  private void init() {
    masterDataBean = FacesContexts.evaluateValueExpression("#{masterDataBean}", MasterDataBean.class);
    viewerBean = FacesContexts.evaluateValueExpression("#{processViewerBean}", ProcessViewerBean.class);
    colorPickerBean = FacesContexts.evaluateValueExpression("#{colorPickerBean}", ColorPickerBean.class);
    processesMap = masterDataBean.getProcessesMap();
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
    selectedColorMode = availableColorMaps.getFirst();
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

  public List<SelectItem> getAvailableProcessStarts() {
    if (StringUtils.isBlank(selectedModule)) {
      return new ArrayList<>();
    }
    List<SelectItem> processStartsSelection = new ArrayList<>();
    processesMap.get(selectedModule).stream()
        .filter(process -> CollectionUtils.isNotEmpty(process.getStartElements()))
        .forEach(process -> {
          if (process.getStartElements().size() == 1) {
            var item = createNewProcessItemForDropdown(process, process.getStartElements().getFirst());
            var processNameAndStartElement = process.getName().concat(ProcessAnalyticsConstants.SLASH).concat(item.getLabel());
            item.setLabel(processNameAndStartElement);
            processStartsSelection.add(item);
            return;
          }

          var processStart = new ProcessAnalyser(process);
          var group = new SelectItemGroup(process.getName());
          group.setValue(processStart);
          SelectItem[] startElementsSelection = process.getStartElements().stream()
              .map(startElement -> createNewProcessItemForDropdown(process, startElement))
              .toArray(SelectItem[]::new);
          group.setSelectItems(startElementsSelection);
          processStartsSelection.add(group);
        });

    return processStartsSelection;
  }

  private SelectItem createNewProcessItemForDropdown(Process process, StartElement startElement) {
    var processStartElement = new ProcessAnalyser(process, startElement);
    String displayName = getStartElementDisplayName(startElement);
    var description = process.getName().concat(ProcessAnalyticsConstants.SLASH).concat(displayName);
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

  private boolean isDiagramAndStatisticRenderable() {
    return ObjectUtils.allNotNull(selectedProcessAnalyser, selectedKpiType);
  }

  public void onModuleSelect() {
    selectedProcessAnalyser = null;
    PF.current().ajax().update(ProcessAnalyticViewComponentId.PROCESS_SELECTION_GROUP);
    resetStatisticValue();
  }

  public void onProcessSelect() {
    resetStatisticValue();
    if (selectedProcessAnalyser != null) {
      getCaseAndTaskCustomFields();
      refreshAnalyserReportToView();
    }
  }

  public void onKpiTypeSelect() {
    colorPickerBean.initBean(selectedKpiType, selectedColorMode);
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
    selectedPid = selectedProcessAnalyser.getProcess().getId();
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
    renderNodesForKPIType();
  }

  public void updateDataOnChangingFilter() {
    var parameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String from = parameterMap.get(ProcessAnalyticsConstants.FROM);
    String to = parameterMap.get(ProcessAnalyticsConstants.TO);
    timeIntervalFilter.setFrom(DateUtils.parseDateFromString(from));
    timeIntervalFilter.setTo(DateUtils.parseDateFromString(to));
    resetStatisticValue();
    getCaseAndTaskCustomFields();
    refreshAnalyserReportToView();
  }

  public void updateDiagramAndStatistic() {
    if (isDiagramAndStatisticRenderable()) {
      viewerBean.init(selectedProcessAnalyser);
      loadNodes();
      updateProcessMiningDataJson();
      PF.current().executeScript(ProcessAnalyticsConstants.UPDATE_IFRAME_SOURCE_METHOD_CALL);
      PF.current().ajax().update(ProcessAnalyticViewComponentId.getDiagramAndStatisticComponentIds());
    }
  }
  
  public void onColorModeChange() {
    if (this.selectedKpiType == null) {
      return;
    }

    if(COLOR_MAP_HEATMAP.equals(selectedColorMode)) {
      colorPickerBean.onChooseHeatMapMode();
    } else {
      colorPickerBean.onChooseColorChooserMode();
    }
    refreshAnalyserReportToView();
  }

  private void loadNodes() {
    analyzedNode = new ArrayList<>();
    selectedPid = selectedProcessAnalyser.getProcess().getId();
    initializingProcessMiningData();

    if (haveMandatoryFieldsBeenFilled()) {
      Long taskStartId = selectedProcessAnalyser.getStartElement().getTaskStartId();
      List<ICase> cases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(taskStartId,
          timeIntervalFilter, selectedCustomFilters, isIncludingRunningCases);
      if (CollectionUtils.isNotEmpty(cases)) {
        analyzedNode = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(selectedProcessAnalyser, selectedKpiType, cases);
        processMiningData.setNodes(analyzedNode);
        processMiningData.setNumberOfInstances(cases.size());
      }
    }
    updateDataTableWithNodesPrefix(ProcessUtils.getProcessPidFromElement(selectedPid));
  }

  private boolean haveMandatoryFieldsBeenFilled() {
    return StringUtils.isNoneBlank(selectedModule)
        && ObjectUtils.allNotNull(selectedKpiType, selectedProcessAnalyser);
  }

  private void updateProcessMiningDataJson() {
    String jsonString = JacksonUtils.convertObjectToJSONString(processMiningData);
    processMiningDataJsonFile.value().get(ProcessAnalyticsConstants.EN_CMS_LOCALE).write().string(jsonString);
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
    String formattedKpiTypeName = selectedKpiType.getCmsName()
        .replaceAll(ProcessAnalyticsConstants.SPACE_DASH_REGEX, ProcessAnalyticsConstants.UNDERSCORE)
        .replaceAll(ProcessAnalyticsConstants.MULTIPLE_UNDERSCORES_REGEX, ProcessAnalyticsConstants.UNDERSCORE);
    var startName = Optional.ofNullable(selectedProcessAnalyser).map(ProcessAnalyser::getStartElement)
        .map(StartElement::getName).orElse(StringUtils.EMPTY);
    return String.format(ProcessAnalyticsConstants.ANALYSIS_EXCEL_FILE_PATTERN, formattedKpiTypeName, startName);
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
 
  public List<String> getAvailableColorMaps() {
    return availableColorMaps;
  }

  public String getSelectedColorMode() {
    return selectedColorMode;
  }

  public void setSelectedColorMode(String selectedColorMode) {
    this.selectedColorMode = selectedColorMode;
  }
}
