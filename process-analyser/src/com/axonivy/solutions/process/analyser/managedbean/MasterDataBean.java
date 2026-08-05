package com.axonivy.solutions.process.analyser.managedbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import org.apache.commons.lang3.Strings;
import org.primefaces.PF;

import com.axonivy.solutions.process.analyser.bo.IvyProcess;
import com.axonivy.solutions.process.analyser.bo.ProcessAnalyser;
import com.axonivy.solutions.process.analyser.bo.ProcessViewerConfig;
import com.axonivy.solutions.process.analyser.bo.StartElement;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.ANALYSIS_EXCEL_FILE_PATTERN;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.MULTIPLE_UNDERSCORES_REGEX;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.SPACE_DASH_REGEX;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.UNDERSCORE;
import com.axonivy.solutions.process.analyser.constants.CoreConstants;
import static com.axonivy.solutions.process.analyser.constants.CoreConstants.HYPHEN_SIGN;
import com.axonivy.solutions.process.analyser.constants.ProcessAnalyticViewComponentId;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.StartElementType;
import static com.axonivy.solutions.process.analyser.enums.StartElementType.StartEventElement;
import static com.axonivy.solutions.process.analyser.enums.StartElementType.StartSignalEventElement;
import static com.axonivy.solutions.process.analyser.enums.StartElementType.WebServiceProcessStartElement;
import com.axonivy.solutions.process.analyser.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.application.app.Application;
import ch.ivyteam.ivy.application.project.Project;
import ch.ivyteam.ivy.environment.Ivy;
import jakarta.annotation.PostConstruct;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class MasterDataBean implements Serializable {

  private static final long serialVersionUID = 1L;
  private Set<String> availableModules;
  List<SelectItem> availableProcessStarts;
  // private Project selectedPMV;
  private boolean isMergeProcessStarts;
  private String selectedModule;
  private ProcessAnalyser selectedProcessAnalyser;
  private KpiType selectedKpiType;
  private List<SelectItem> kpiTypes;
  private Set<String> availableRoles;
  private boolean isWidgetMode;
  private String selectedRole;
  private boolean isIncludingRunningCases;
  private String processSelectionGroupId;
  private String pmvGroupId;
  private String roleSelectionGroupId;
  private List<IvyProcess> availableProcesses;

  @PostConstruct
  public void init() {
    isMergeProcessStarts = true;
    isWidgetMode = false;
    initKpiTypes();
    availableModules = ProcessUtils.getAllAvaiableModule();
    availableRoles = Set.of();
    processSelectionGroupId = ProcessAnalyticViewComponentId.PROCESS_SELECTION_GROUP;
    pmvGroupId = ProcessAnalyticViewComponentId.PMV_GROUP;
    roleSelectionGroupId = ProcessAnalyticViewComponentId.ROLE_SELECTION_GROUP;
    availableProcesses = new ArrayList<>();
  }

  public void initSelectedValueFromUserProperty() {
    isWidgetMode = true;
    processSelectionGroupId = ProcessAnalyticViewComponentId.WIDGET_PROCESS_SELECTION_GROUP;
    pmvGroupId = ProcessAnalyticViewComponentId.WIDGET_PMV_GROUP;
    roleSelectionGroupId = ProcessAnalyticViewComponentId.WIDGET_ROLE_SELECTION_GROUP;
    ProcessViewerConfig persistedConfig = ProcessesMonitorUtils.getUserConfig();
    selectedRole =
        availableRoles.stream().filter(role -> Strings.CS.equals(persistedConfig.getWidgetSelectedRole(), role)).findAny().orElse(null);
    selectedModule = persistedConfig.getWidgetSelectedModule();
    isMergeProcessStarts = BooleanUtils.isTrue(persistedConfig.getWidgetMergedProcessStart());
    isIncludingRunningCases = BooleanUtils.isTrue(persistedConfig.getWidgetIncludeRunningCase());
    Project selectedProject = getAvailablePMV().stream()
        .filter(pmv -> Strings.CS.equals(pmv.name(), persistedConfig.getWidgetSelectedPmv()))
        .findAny().orElse(null);
    availableProcesses = ProcessUtils.getAllProcessByModule(selectedProject);
    String selectedKpiTypeName = persistedConfig.getWidgetSelectedKpi();
    String selectedProcessAnalyzerId = persistedConfig.getWidgetSelectedProcessAnalyzer();
    if (StringUtils.isNoneBlank(selectedModule, selectedProcessAnalyzerId)) {
      selectedProcessAnalyser = initSelectedProcessAnalyser(selectedProcessAnalyzerId);
    }
    if (StringUtils.isNotBlank(selectedKpiTypeName)) {
      selectedKpiType = KpiType.valueOf(selectedKpiTypeName);
    }
  }

  private ProcessAnalyser initSelectedProcessAnalyser(String selectedProcessAnalyzerId) {
    ProcessAnalyser persistedProcessAnalyser = new ProcessAnalyser();
    String[] parts = selectedProcessAnalyzerId.split(HYPHEN_SIGN, 2);
    if (parts.length >= 1) {
      String selectedProcessId = parts[0];
      var selectedProcess =
          availableProcesses.stream().filter(process -> Strings.CS.equals(process.getId(), selectedProcessId)).findAny().orElse(null);
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

  public List<SelectItem> getAvailableProcessStarts() {
    if (StringUtils.isBlank(selectedModule)) {
      return new ArrayList<>();
    }
    List<SelectItem> processStartsSelection = new ArrayList<>();
    availableProcesses.stream().filter(process -> CollectionUtils.isNotEmpty(process.getStartElements())).forEach(process -> {
      if (isMergeProcessStarts) {
        addMergeProcessStart(process, processStartsSelection);
      } else {
        handleProcessStarts(process, processStartsSelection);
      }
    });
    return processStartsSelection;
  }

  public String generateNameOfExcelFile() {
    String formattedKpiTypeName =
        selectedKpiType.getCmsName().replaceAll(SPACE_DASH_REGEX, UNDERSCORE).replaceAll(MULTIPLE_UNDERSCORES_REGEX, UNDERSCORE);
    var startName =
        Optional.ofNullable(selectedProcessAnalyser)
            .map(analyser -> isMergeProcessStarts ? Optional.ofNullable(analyser.getProcess()).map(IvyProcess::getName).orElse(EMPTY)
                : Optional.ofNullable(analyser.getStartElement()).map(StartElement::getName).orElse(EMPTY))
            .orElse(EMPTY);
    return String.format(ANALYSIS_EXCEL_FILE_PATTERN, formattedKpiTypeName, startName);
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

  private void addMergeProcessStart(IvyProcess process, List<SelectItem> processStartsSelection) {
    var processItem = new SelectItem(new ProcessAnalyser(process), process.getName(), process.getName());
    processStartsSelection.add(processItem);
  }

  private void handleProcessStarts(IvyProcess process, List<SelectItem> processStartsSelection) {
    if (process.getStartElements().size() == 1) {
      addSingleStartElement(process, processStartsSelection);
    } else {
      addMultipleStartElements(process, processStartsSelection);
    }
  }

  private void addSingleStartElement(IvyProcess process, List<SelectItem> processStartsSelection) {
    var startElement = process.getStartElements().getFirst();
    var item = createNewProcessItemForDropdown(process, startElement);

    var processNameAndStartElement = process.getName().concat(CoreConstants.SLASH).concat(item.getLabel());
    item.setLabel(processNameAndStartElement);

    processStartsSelection.add(item);
  }

  private void addMultipleStartElements(IvyProcess process, List<SelectItem> processStartsSelection) {
    var group = new SelectItemGroup(process.getName());
    group.setValue(new ProcessAnalyser(process));

    SelectItem[] startElementsSelection = process.getStartElements().stream()
        .map(startElement -> createNewProcessItemForDropdown(process, startElement)).toArray(SelectItem[]::new);

    group.setSelectItems(startElementsSelection);
    processStartsSelection.add(group);
  }

  private SelectItem createNewProcessItemForDropdown(IvyProcess process, StartElement startElement) {
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

  public List<Project> getAvailablePMV() {
    if (StringUtils.isEmpty(selectedModule)) {
      return List.of();
    }

    // TODO Need to be corrected after migrating to Jakarta
    return Application.current().projects()
        .readyModels()
        .map(Project::of)
        .toList();
  }

  // private void resetDefaultPMV() {
  //   List<Project> pmvs = getAvailablePMV().stream()
  //       // TODO Need to be corrected after migrating to Jakarta
  //       // .filter(version -> version.isReady())
  //       .sorted(Comparator.comparing(Project::getLastChangeDate).reversed()).toList();
  //   selectedPMV = ObjectUtils.isNotEmpty(pmvs) ? pmvs.get(0) : null;
  // }

  // public Project getSelectedPMV() {
  //   return selectedPMV;
  // }

  public void handleModuleChange() {
    if (isWidgetMode) {
      ProcessesMonitorUtils.updateUserConfig(persistedConfig -> persistedConfig.setWidgetSelectedModule(selectedModule));
    }
    // resetDefaultPMV();
    // handlePmvChange();
    PF.current().ajax().update(pmvGroupId);
  }

  public void handleProcessChange() {
    if (isWidgetMode) {
      ProcessesMonitorUtils.updateUserConfig(
          persistedConfig -> persistedConfig.setWidgetSelectedProcessAnalyzer(getWidgetSelectedProcessAnalyzerKey()));
    }
    selectedRole = null;
    availableRoles = ProcessesMonitorUtils.getActivatorRoleNameFromProcess(selectedProcessAnalyser);
    handleRoleChange();
    PF.current().ajax().update(roleSelectionGroupId);
  }

  // public void handlePmvChange() {
  //   if (isWidgetMode) {
  //     ProcessesMonitorUtils.updateUserConfig(persistedConfig -> persistedConfig.setWidgetSelectedPmv(selectedPMV.name()));
  //   }
  //   selectedProcessAnalyser = null;
  //   availableProcesses = ProcessUtils.getAllProcessByModule(selectedModule, selectedPMV);
  //   handleProcessChange();
  //   PF.current().ajax().update(processSelectionGroupId);
  // }

  public void handleMergeProcessStartsChange() {
    selectedProcessAnalyser = null;
    if (isWidgetMode) {
      ProcessesMonitorUtils
          .updateUserConfig(persistedConfig -> persistedConfig.setWidgetMergedProcessStart(isMergeProcessStarts));
    }
  }

  public void handleKpiTypeChange() {
    if (isWidgetMode) {
      ProcessesMonitorUtils
          .updateUserConfig(persistedConfig -> persistedConfig.setWidgetSelectedKpi(selectedKpiType.name()));
    }
  }

  public void handleRoleChange() {
    if (isWidgetMode) {
      ProcessesMonitorUtils.updateUserConfig(persistedConfig -> persistedConfig.setWidgetSelectedRole(selectedRole));
    }
  }

  public String getWidgetSelectedProcessAnalyzerKey() {
    if (selectedProcessAnalyser == null) {
      return null;
    }
    String selectedProcessId = Optional.ofNullable(selectedProcessAnalyser.getProcess()).map(IvyProcess::getId).orElse(EMPTY);
    String selectedStartId = Optional.ofNullable(selectedProcessAnalyser.getStartElement()).map(StartElement::getPid).orElse(EMPTY);
    return isMergeProcessStarts ? selectedProcessId : String.join(HYPHEN_SIGN, selectedProcessId, selectedStartId);
  }

  public String getPmvLabel(Project pmv) {
    return Ivy.cms().co("/Dialogs/com/axonivy/solutions/process/analyser/ProcessesMonitor/Version", List.of(pmv.name()));
  }

  public boolean isStatisticReportRenderable() {
    return ObjectUtils.allNotNull(selectedKpiType, selectedProcessAnalyser) && StringUtils.isNotBlank(selectedModule);
  }

  // public void setSelectedPMV(Project selectedPMV) {
  //   this.selectedPMV = selectedPMV;
  // }

  public boolean isDurationKpiType() {
    return ProcessesMonitorUtils.isDuration(selectedKpiType);
  }

  public List<SelectItem> getKpiTypes() {
    return kpiTypes;
  }

  public KpiType getSelectedKpiType() {
    return selectedKpiType;
  }

  public void setSelectedKpiType(KpiType selectedKpiType) {
    this.selectedKpiType = selectedKpiType;
  }

  public String getSelectedModule() {
    return selectedModule;
  }

  public void setSelectedModule(String selectedModule) {
    this.selectedModule = selectedModule;
  }

  public ProcessAnalyser getSelectedProcessAnalyser() {
    return selectedProcessAnalyser;
  }

  public void setSelectedProcessAnalyser(ProcessAnalyser selectedProcessAnalyser) {
    this.selectedProcessAnalyser = selectedProcessAnalyser;
  }

  public List<IvyProcess> getAvailableProcesses() {
    return availableProcesses;
  }

  public void setAvailableProcesses(List<IvyProcess> availableProcesses) {
    this.availableProcesses = availableProcesses;
  }

  public void setAvailableProcessStarts(List<SelectItem> availableProcessStarts) {
    this.availableProcessStarts = availableProcessStarts;
  }

  public Set<String> getAvailableModules() {
    return availableModules;
  }

  public void setAvailableModules(Set<String> availableModules) {
    this.availableModules = availableModules;
  }

  public boolean isMergeProcessStarts() {
    return isMergeProcessStarts;
  }

  public void setMergeProcessStarts(boolean isMergeProcessStarts) {
    this.isMergeProcessStarts = isMergeProcessStarts;
  }

  public void setKpiTypes(List<SelectItem> kpiTypes) {
    this.kpiTypes = kpiTypes;
  }

  public boolean isWidgetMode() {
    return isWidgetMode;
  }

  public void setWidgetMode(boolean isWidgetMode) {
    this.isWidgetMode = isWidgetMode;
  }

  public Set<String> getAvailableRoles() {
    return availableRoles;
  }

  public void setAvailableRoles(Set<String> availableRoles) {
    this.availableRoles = availableRoles;
  }

  public String getSelectedRole() {
    return selectedRole;
  }

  public void setSelectedRole(String selectedRole) {
    this.selectedRole = selectedRole;
  }

  public boolean isIncludingRunningCases() {
    return isIncludingRunningCases;
  }

  public void setIncludingRunningCases(boolean isIncludingRunningCases) {
    this.isIncludingRunningCases = isIncludingRunningCases;
  }
}
