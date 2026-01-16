package com.axonivy.solutions.process.analyser.core.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.bo.StartElement;
import com.axonivy.solutions.process.analyser.core.constants.CoreConstants;
import com.axonivy.solutions.process.analyser.core.util.PIDUtils;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.ProcessKind;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.activity.SubProcessCall;
import ch.ivyteam.ivy.process.model.element.event.end.CallSubEnd;
import ch.ivyteam.ivy.process.model.element.event.end.EmbeddedEnd;
import ch.ivyteam.ivy.process.model.element.event.end.TaskEnd;
import ch.ivyteam.ivy.process.model.element.event.intermediate.TaskSwitchEvent;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.model.element.event.start.EmbeddedStart;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.Join;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.element.value.task.ResponsibleType;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;
import ch.ivyteam.ivy.process.model.value.PID;
import ch.ivyteam.ivy.process.rdm.IProcess;
import ch.ivyteam.ivy.process.rdm.IProcessManager;
import ch.ivyteam.ivy.process.rdm.IProjectProcessManager;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.IProcessStart;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@SuppressWarnings("restriction")
public class ProcessUtils {

  static final String SKIP_PROJECTS_VARIABLE = "com.axonivy.solutions.process.analyser.skipProjects";
  static final String SKIP_PROCESSES_VARIABLE = "com.axonivy.solutions.process.analyser.skipProcesses";

  private ProcessUtils() { }

  public static String getElementPid(BaseElement baseElement) {
    return Optional.ofNullable(baseElement).map(BaseElement::getPid).map(PID::toString).orElse(StringUtils.EMPTY);
  }

  public static String getProcessPidFromElement(String elementId) {
    return StringUtils.defaultString(elementId).split(CoreConstants.HYPHEN_SIGN)[0];
  }

  public static boolean isEmbeddedElementInstance(Object element) {
    return EmbeddedProcessElement.class.isInstance(element);
  }

  public static boolean isAlternativeInstance(Object element) {
    return Alternative.class.isInstance(element);
  }

  public static boolean isTaskSwitchInstance(Object element) {
    return TaskSwitchEvent.class.isInstance(element);
  }

  public static boolean isTaskEndInstance(Object element) {
    return TaskEnd.class.isInstance(element);
  }

  public static boolean isTaskSwitchGatewayInstance(Object element) {
    return TaskSwitchGateway.class.isInstance(element);
  }

  public static boolean isSubProcessCallInstance(Object element) {
    return SubProcessCall.class.isInstance(element);
  }

  public static boolean isEmbeddedEndInstance(Object element) {
    return EmbeddedEnd.class.isInstance(element);
  }

  public static List<ProcessElement> getNestedProcessElementsFromSub(Object element) {
    return switch (element) {
    case EmbeddedProcessElement embeddedElement -> getEmbbedProcessElements(embeddedElement).stream()
        .flatMap(e -> Stream.concat(Stream.of(e), getEmbbedProcessElements(e).stream())).collect(Collectors.toList());
    case SubProcessCall subProcessCall ->
      getProcessElementsFromCallableSubProcessPath(subProcessCall.getCallTarget().getProcessName().getName());
    default -> Collections.emptyList();
    };
  }

  /*
   * Get nested process elements inside the sub (we only support 2 nested layer)
   */
  public static List<ProcessElement> getEmbbedProcessElements(ProcessElement processElement) {
    if (processElement instanceof EmbeddedProcessElement embeddedElement) {
      return embeddedElement.getEmbeddedProcess().getProcessElements();
    }
    return Collections.emptyList();
  }

  public static ProcessElement getStartElementFromSubProcessCall(Object element) {
    if (!isSubProcessCallInstance(element)) {
      return null;
    }
    String targetName = SubProcessCall.class.cast(element).getCallTarget().getSignature().getName();
    return getNestedProcessElementsFromSub(element).stream().filter(CallSubStart.class::isInstance)
        .map(CallSubStart.class::cast).filter(start -> Strings.CS.equals(start.getSignature().getName(), targetName))
        .findAny().orElse(null);
  }

  /*
   * Get nested process elements inside the call-able sub by these steps: 1) find
   * the process path which is called inside the sub 2) find all of process element
   * from this process 3) (Optional) find nested embedded process inside the BPMN
   * sub (if exist)
   */
  private static List<ProcessElement> getProcessElementsFromCallableSubProcessPath(String subProcessPath) {
    return IProcessManager.instance().getProjectDataModels().stream()
        .map(model -> model.getProcessByPath(subProcessPath)).filter(Objects::nonNull).findAny()
        .map(process -> process.getModel().getProcessElements().stream()
            .flatMap(pe -> Stream.concat(Stream.of(pe), getNestedProcessElementsFromSub(pe).stream()))
            .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }

  public static List<ProcessElement> getProcessElementsFrom(String processId, IProcessModelVersion pmv) {
    if (StringUtils.isBlank(processId)) {
      return Collections.emptyList();
    }

    String processRawPid = getProcessPidFromElement(processId);
    IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(pmv);
    IProcess foundProcess = manager.findProcess(processRawPid, true);
    if (foundProcess == null) {
      return Collections.emptyList();
    }

    // Get all process elements, including nested ones
    return foundProcess.getModel().getProcessElements().stream()
        .flatMap(element -> Stream.concat(Stream.of(element), getNestedProcessElementsFromSub(element).stream()))
        .collect(Collectors.toList());
  }

  public static List<SequenceFlow> getSequenceFlowsFrom(List<ProcessElement> elements) {
    return elements.stream().flatMap(element -> element.getOutgoing().stream()).collect(Collectors.toList());
  }

  public static List<Process> getAllProcesses() {
    String configSkipProcesses = StringUtils.trim(Ivy.var().get(SKIP_PROCESSES_VARIABLE));
    String[] skipProcesses = Arrays.asList(StringUtils.split(configSkipProcesses, CoreConstants.SEMI_COLONS))
        .stream().filter(StringUtils::isNotBlank)
        .map(String::trim).toArray(String[]::new);
    List<Process> processes = new ArrayList<>();
    for (var pmv : getProcessModelVersionsInCurrentApp()) {
      List<IProcessStart> processStarts = getProcessStartsForPMV(pmv);
      // Index process starts by processFileId for fast lookup
      Map<String, List<IProcessStart>> startsByProcessId = processStarts.stream()
        .collect(Collectors.groupingBy(start -> PIDUtils.getId(start.pid(), true)));

      for (var processFile : getProcessesInCurrentPMV(pmv)) {
        String processFileId = processFile.getIdentifier();
        var process = new Process(processFileId, processFile.getName(), new ArrayList<>());
        if (Strings.CI.equalsAny(process.getName(), skipProcesses)){
          continue;
        }
        process.setPmvId(pmv.getId());
        process.setPmvName(pmv.getName());
        process.setPmv(pmv);
        process.setProjectRelativePath(processFile.getResource().getProjectRelativePath().toString());

        List<IProcessStart> starts = startsByProcessId.getOrDefault(processFileId, Collections.emptyList());

        if (CollectionUtils.isEmpty(starts)) {
          continue;
        }

        for (var start : starts) {
          var taskStart = start.getTaskStart();
          StartElement startElement = new StartElement();
          startElement.setPid(PIDUtils.getId(taskStart.getProcessElementId()));
          startElement.setTaskStartId(taskStart.getId());
          ProcessStartFactory.extractDisplayNameAndType(start, startElement);
          process.getStartElements().add(startElement);
        }
        processes.add(process);
      }
    }
    return processes;
  }

  private static List<IProcess> getProcessesInCurrentPMV(IProcessModelVersion pmv) {
    return IProcessManager.instance().getProjectDataModelFor(pmv).getProcesses().stream()
        .filter(process -> process.getKind() == ProcessKind.NORMAL || process.getKind() == ProcessKind.WEB_SERVICE)
        .toList();
  }

  private static List<IProcessModelVersion> getProcessModelVersionsInCurrentApp() {
    return IApplication.current().getProcessModelVersions()
        .filter(isPMVNeedToRecordStatistic())
        .sorted((pmv1, pmv2) -> pmv1.getName().compareTo(pmv2.getName()))
        .toList();
  }

  private static Predicate<? super IProcessModelVersion> isPMVNeedToRecordStatistic() {
    String configSkipProjects = StringUtils.trim(Ivy.var().get(SKIP_PROJECTS_VARIABLE));
    String[] skipPMVs = Arrays.asList(StringUtils.split(configSkipProjects, CoreConstants.SEMI_COLONS))
        .stream().filter(StringUtils::isNoneBlank)
        .map(String::trim).toArray(String[]::new);
    return pmv -> {
      String pmName = pmv.getProcessModel().getName();
      return !(Strings.CS.equals(pmName, CoreConstants.PROCESS_ANALYSER_PMV_NAME)
          || Strings.CS.contains(pmName, CoreConstants.PORTAL_PMV_SUFFIX)
          || Strings.CI.equalsAny(pmName, skipPMVs));
    };
  }

  @SuppressWarnings("removal")
  private static List<IProcessStart> getProcessStartsForPMV(IProcessModelVersion pmv) {
    return Sudo.get(() -> {
      return IWorkflowProcessModelVersion.of(pmv).getProcessStarts();
    });
  }
  
  public static Set<String> getAllAvaiableModule() {
    return getProcessModelVersionsInCurrentApp().stream().map(IProcessModelVersion::getName)
        .collect(Collectors.toSet());
  }

  public static List<Process> getAllProcessByModule(String selectedModule, IProcessModelVersion pmv) {
    List<Process> processes = new ArrayList<>();
    if (StringUtils.isEmpty(selectedModule) || null == pmv ) {
      return processes;
    }

    List<IProcessStart> processStarts = getProcessStartsForPMV(pmv);
    // Index process starts by processFileId for fast lookup
    Map<String, List<IProcessStart>> startsByProcessId =
        processStarts.stream().collect(Collectors.groupingBy(start -> PIDUtils.getId(start.pid(), true)));
    for (var processFile : getProcessesInCurrentPMV(pmv)) {
      String processFileId = processFile.getIdentifier();
      var process = new Process(processFileId, processFile.getName(), new ArrayList<>());
      process.setPmvId(pmv.getId());
      process.setPmvName(pmv.getName());
      process.setPmv(pmv);
      process.setProjectRelativePath(processFile.getResource().getProjectRelativePath().toString());

      List<IProcessStart> starts = startsByProcessId.getOrDefault(processFileId, Collections.emptyList());
      if (CollectionUtils.isEmpty(starts)) {
        continue;
      }

      for (var start : starts) {
        var taskStart = start.getTaskStart();
        StartElement startElement = new StartElement();
        startElement.setPid(PIDUtils.getId(taskStart.getProcessElementId()));
        startElement.setTaskStartId(taskStart.getId());
        ProcessStartFactory.extractDisplayNameAndType(start, startElement);
        process.getStartElements().add(startElement);
      }
      processes.add(process);
    }
    return processes;
  }

  public static boolean isIWebStartableNeedToRecordStatistic(IWebStartable process) {
    String pmName = process.pmv().getProcessModel().getName();
    return !(Strings.CS.equals(pmName, CoreConstants.PROCESS_ANALYSER_PMV_NAME)
        || Strings.CS.contains(pmName, CoreConstants.PORTAL_PMV_SUFFIX))
        && IProcessWebStartable.class.isInstance(process);
  }

  public static List<ProcessElement> getAlterNativesWithMultiOutgoings(List<ProcessElement> processElements) {
    return Optional.ofNullable(processElements).orElse(new ArrayList<>()).stream()
        .filter(element -> isAlternativeInstance(element) && element.getOutgoing().size() > 1)
        .collect(Collectors.toList());
  }

  public static List<ProcessElement> getElementsWithMultiIncomings(List<ProcessElement> processElements) {
    return Optional.ofNullable(processElements).orElse(new ArrayList<>()).stream()
        .filter(ProcessUtils::isComplexElementWithMultiIncomings).collect(Collectors.toList());
  }

  public static List<ProcessElement> getTaskStart(List<ProcessElement> processElements) {
    return Optional.ofNullable(processElements).orElse(new ArrayList<>()).stream()
        .filter(ProcessUtils::isTaskStartElement).collect(Collectors.toList());
  }

  public static boolean isTaskStartElement(ProcessElement element) {
    return taskConfigsOf(element).isPresent();
  }

  public static boolean isComplexElementWithMultiIncomings(ProcessElement element) {
    return switch (element) {
    case Join join -> false;
    case EmbeddedProcessElement embeddedProcessElement -> false;
    case SubProcessCall subProcessCall -> false;
    default -> isElementWithMultipleIncomingFlow(element);
    };
  }

  private static Optional<List<TaskConfig>> taskConfigsOf(ProcessElement element) {
    List<TaskConfig> configs = switch (element) {
    case TaskSwitchGateway taskSwitchGateway -> taskSwitchGateway.getAllTaskConfigs();
    case TaskSwitchEvent taskSwitchEvent -> taskSwitchEvent.getAllTaskConfigs();
    case RequestStart requestStart -> requestStart.getAllTaskConfigs();
    default -> null;
    };
    return Optional.ofNullable(configs);
  }

  public static Set<String> getActivatorFromTaskConfigs(List<TaskConfig> taskConfigs) {
    Set<String> configs = taskConfigs.stream().map(TaskConfig::responsible)
        .filter(responsible -> responsible.type() == ResponsibleType.ROLES && CollectionUtils.isNotEmpty(responsible.roles()))
        .map(t -> t.roles().getFirst()).collect(Collectors.toSet());
    return configs;
  }

  public static Set<String> getTaskActivatorAsRoleName(ProcessElement element) {
    return taskConfigsOf(element).map(ProcessUtils::getActivatorFromTaskConfigs).orElse(Set.of());
  }

  public static List<ProcessElement> getTaskSwitchEvents(List<ProcessElement> processElements) {
    return Optional.ofNullable(processElements).orElse(Collections.emptyList()).stream()
        .filter(element -> isTaskSwitchInstance(element)).toList();
  }

  @SuppressWarnings("removal")
  public static Long getTaskStartIdFromPID(String rawPid) {
    return Ivy.session().getStartableProcessStarts().stream()
        .filter(start -> Strings.CS.equals(rawPid, start.getProcessElementId())).findFirst().map(IProcessStart::getId)
        .orElse(0L);
  }

  public static String getTaskElementIdFromRequestPath(String requestPath) {
    String[] arr = requestPath.split(CoreConstants.SLASH);
    // Request Path contains: {PROCESS ID}/.../{NAME OF TASK}
    // So we have get the node before /{NAME OF TASK}
    // Ignore case {PROCESS ID}/{NAME OF TASK}
    return arr.length > 2 ? arr[arr.length - 2] : StringUtils.EMPTY;
  }

  public static String getTaskElementId(ITask task) {
    return Optional.ofNullable(task).map(ITask::getRequestPath).map(ProcessUtils::getTaskElementIdFromRequestPath)
        .orElse(StringUtils.EMPTY);
  }

  public static String getTaskElementIdFromRequestPath(String requestPath, boolean isTaskInTaskSwitchGateway) {
    if (!isTaskInTaskSwitchGateway) {
      return getTaskElementIdFromRequestPath(requestPath);
    }
    String[] arr = requestPath.split(CoreConstants.SLASH);
    return arr.length > 2 ? arr[arr.length - 2] + CoreConstants.SLASH + arr[arr.length - 1]
        : StringUtils.EMPTY;
  }

  public static boolean isElementWithMultipleIncomingFlow(ProcessElement processElement) {
    return Optional.ofNullable(processElement).map(element -> element.getIncoming().size() > 1).orElse(false);
  }

  public static boolean isProcessPathEndElement(ProcessElement processElement) {
    return Optional.ofNullable(processElement).map(element -> CollectionUtils.isEmpty(element.getOutgoing()))
        .orElse(false);
  }

  public static boolean isAlternativePathEndElement(ProcessElement processElement) {
    return switch (processElement) {
    case Alternative alternative -> true;
    case CallSubEnd callSubEnd -> false;
    case Join join -> false;
    case EmbeddedProcessElement sub -> false;
    case EmbeddedEnd subEnd -> false;
    default -> isProcessPathEndElement(processElement) || isElementWithMultipleIncomingFlow(processElement);
    };
  }

  public static String getSelectedProcessFilePath(String selectedStartableId, String selectedModule,
      String applicationName) {
    String processFilePath = selectedStartableId.replace(
        String.format(CoreConstants.MODULE_PATH, applicationName, selectedModule), StringUtils.EMPTY);
    int lastSlashIndex = processFilePath.lastIndexOf(CoreConstants.SLASH);
    if (lastSlashIndex != StringUtils.INDEX_NOT_FOUND) {
      processFilePath = processFilePath.substring(0, lastSlashIndex) + CoreConstants.PROCESSFILE_EXTENSION;
    }
    return processFilePath;
  }

  public static boolean isEmbeddedStartConnectToSequenceFlow(ProcessElement element, String targetSequenceFlowId) {
    if (element instanceof EmbeddedStart embeddedStart) {
      String connectedOutterFlowId = embeddedStart.getConnectedOuterSequenceFlow().getPid().toString();
      return StringUtils.defaultString(targetSequenceFlowId).equals(connectedOutterFlowId);
    }
    return false;
  }

  public static ProcessElement getEmbeddedStartConnectToFlow(ProcessElement processElement, String outerFlowId) {
    if (isEmbeddedElementInstance(processElement)) {
      processElement = getNestedProcessElementsFromSub(processElement).stream()
          .filter(element -> isEmbeddedStartConnectToSequenceFlow(element, outerFlowId)).findAny().orElse(null);
    }
    return processElement;
  }
}
