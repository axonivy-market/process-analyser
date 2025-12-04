package com.axonivy.solutions.process.analyser.managedbean;

import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.ANALYSIS_EXCEL_FILE_PATTERN;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.MULTIPLE_UNDERSCORES_REGEX;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.SPACE_DASH_REGEX;
import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.UNDERSCORE;
import static com.axonivy.solutions.process.analyser.core.enums.StartElementType.StartEventElement;
import static com.axonivy.solutions.process.analyser.core.enums.StartElementType.StartSignalEventElement;
import static com.axonivy.solutions.process.analyser.core.enums.StartElementType.WebServiceProcessStartElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PF;

import com.axonivy.solutions.process.analyser.bo.ProcessAnalyser;
import com.axonivy.solutions.process.analyser.bo.ProcessViewerConfig;
import com.axonivy.solutions.process.analyser.constants.ProcessAnalyticViewComponentId;
import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.bo.StartElement;
import com.axonivy.solutions.process.analyser.core.constants.CoreConstants;
import com.axonivy.solutions.process.analyser.core.enums.StartElementType;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.utils.FacesContexts;
import com.axonivy.solutions.process.analyser.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.application.ActivityState;
import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.ILibrary;
import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.application.ReleaseState;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.IRole;

@ManagedBean
@ViewScoped
public class MasterDataBean implements Serializable {

  private static final long serialVersionUID = 1L;
  private Set<String> availableModules;
  List<SelectItem> availableProcessStarts;
  private IProcessModelVersion selectedPMV;
  private boolean isMergeProcessStarts;
  private String selectedModule;
  private ProcessAnalyser selectedProcessAnalyser;
  private KpiType selectedKpiType;
  private List<SelectItem> kpiTypes;
  private List<String> availableRoles;
  private boolean isWidgetMode;
  private String selectedRole;

  @PostConstruct
  public void init() {
    isMergeProcessStarts = true;
    var isWidgetModeValue = FacesContexts.evaluateValueExpression("#{data.isWidgetMode}", Boolean.class);
    isWidgetMode = BooleanUtils.isTrue(isWidgetModeValue);
    initKpiTypes();
    availableModules = ProcessUtils.getAllAvaiableModule();
    availableRoles = Ivy.security().roles().all().stream().map(IRole::getName).toList();
  }

  public List<SelectItem> getAvailableProcessStarts() {
    if (StringUtils.isBlank(selectedModule)) {
      return new ArrayList<>();
    }
    List<SelectItem> processStartsSelection = new ArrayList<>();
    avaiableProcesses.stream().filter(process -> CollectionUtils.isNotEmpty(process.getStartElements())).forEach(process -> {
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
            .map(analyser -> isMergeProcessStarts ? Optional.ofNullable(analyser.getProcess()).map(Process::getName).orElse(StringUtils.EMPTY)
                : Optional.ofNullable(analyser.getStartElement()).map(StartElement::getName).orElse(StringUtils.EMPTY))
            .orElse(StringUtils.EMPTY);
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

  public List<IProcessModelVersion> getAvailabelPMV() {
    Predicate<ILibrary> filterReleasedAndActivePmv = library -> {
      IProcessModelVersion pmv = library.getProcessModelVersion();
      ReleaseState pmvState = pmv.getReleaseState();
      return pmv.getVersionName().contains(selectedModule)
          && (pmvState == ReleaseState.ARCHIVED || pmvState == ReleaseState.RELEASED || pmvState == ReleaseState.DEPRECATED);
    };

    if (StringUtils.isEmpty(selectedModule)) {
      return List.of();
    }

    return IApplication.current().getLibraries().stream().filter(filterReleasedAndActivePmv).map(ILibrary::getProcessModelVersion).toList();
  }

  public void resetDefaultPMV() {
    selectedProcessAnalyser = null;
    List<IProcessModelVersion> pmvs = getAvailabelPMV().stream()
        .filter(version -> version.getActivityState() == ActivityState.ACTIVE && version.getReleaseState() == ReleaseState.RELEASED)
        .sorted(Comparator.comparing(IProcessModelVersion::getLastChangeDate).reversed()).toList();

    selectedPMV = ObjectUtils.isNotEmpty(pmvs) ? pmvs.get(0) : null;
  }

  public IProcessModelVersion getSelectedPMV() {
    return selectedPMV;
  }

  public void onModuleSelect() {
    resetDefaultPMV();
    avaiableProcesses = ProcessUtils.getAllProcessByModule(selectedModule, selectedPMV);
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
//    resetStatisticValue();
  }

  public String getPmvLabel(IProcessModelVersion pmv) {
    return Ivy.cms().co("/Dialogs/com/axonivy/solutions/process/analyser/ProcessesMonitor/Version", List.of(pmv.getVersionNumber()));
  }

  public void setSelectedPMV(IProcessModelVersion selectedPMV) {
    this.selectedPMV = selectedPMV;
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

  private List<Process> avaiableProcesses;

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
//    isSelectedProcessAnalyserChange = !Objects.equals(this.selectedProcessAnalyser, selectedProcessAnalyser);
    this.selectedProcessAnalyser = selectedProcessAnalyser;
  }

  public List<Process> getAvaiableProcesses() {
    return avaiableProcesses;
  }

  public void setAvaiableProcesses(List<Process> avaiableProcesses) {
    this.avaiableProcesses = avaiableProcesses;
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

  public List<String> getAvailableRoles() {
    return availableRoles;
  }

  public void setAvailableRoles(List<String> availableRoles) {
    this.availableRoles = availableRoles;
  }

  public String getSelectedRole() {
    return selectedRole;
  }

  public void setSelectedRole(String selectedRole) {
    this.selectedRole = selectedRole;
  }
}
