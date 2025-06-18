package com.axonivy.solutions.process.analyser.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModel;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.htmldialog.IHtmlDialogContext;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.activity.SubProcessCall;
import ch.ivyteam.ivy.process.model.element.event.end.CallSubEnd;
import ch.ivyteam.ivy.process.model.element.event.end.EmbeddedEnd;
import ch.ivyteam.ivy.process.model.element.event.intermediate.TaskSwitchEvent;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.model.element.event.start.EmbeddedStart;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.Join;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.value.PID;
import ch.ivyteam.ivy.process.rdm.IProcess;
import ch.ivyteam.ivy.process.rdm.IProcessManager;
import ch.ivyteam.ivy.process.rdm.IProjectProcessManager;
import ch.ivyteam.ivy.security.ISecurityContext;
import ch.ivyteam.ivy.workflow.IProcessStart;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@SuppressWarnings("restriction")
public class ProcessUtils {
  private ProcessUtils() {
  }

  public static String getElementPid(BaseElement baseElement) {
    return Optional.ofNullable(baseElement).map(BaseElement::getPid).map(PID::toString).orElse(StringUtils.EMPTY);
  }

  public static String getProcessPidFromElement(String elementId) {
    return StringUtils.defaultString(elementId).split(ProcessAnalyticsConstants.HYPHEN_SIGN)[0];
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
        .map(CallSubStart.class::cast).filter(start -> StringUtils.equals(start.getSignature().getName(), targetName))
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

  public static List<ProcessElement> getProcessElementsFrom(IProcessWebStartable startElement) {
    if (Optional.ofNullable(startElement).map(IProcessWebStartable::pid).isEmpty()) {
      return Collections.emptyList();
    }

    String processRawPid = getProcessPidFromElement(startElement.pid().toString());
    IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(startElement.pmv());
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

  public static List<IWebStartable> getAllProcesses() {
    return Ivy.session().getStartables().stream().filter(ProcessUtils::isIWebStartableNeedToRecordStatistic).toList();
  }

  public static Map<String, List<IProcessWebStartable>> getProcessesWithPmv() {
    Map<String, List<IProcessWebStartable>> result = new HashMap<>();
    for (IWebStartable process : getAllProcesses()) {
      String pmvName = process.pmv().getProcessModel().getName();
      result.computeIfAbsent(pmvName, key -> new ArrayList<>()).add((IProcessWebStartable) process);
    }
    return result;
  }

  public static boolean isIWebStartableNeedToRecordStatistic(IWebStartable process) {
    String pmName = process.pmv().getProcessModel().getName();
    return !(StringUtils.equals(pmName, ProcessAnalyticsConstants.PROCESS_ANALYSER_PMV_NAME)
        || StringUtils.contains(pmName, ProcessAnalyticsConstants.PORTAL_PMV_SUFFIX))
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
    return switch (element) {
    case TaskSwitchGateway taskSwitchGateway -> true;
    case TaskSwitchEvent taskSwitchEvent -> true;
    case RequestStart requestStart -> true;
    default -> false;
    };
  }

  public static boolean isComplexElementWithMultiIncomings(ProcessElement element) {
    return switch (element) {
    case Join join -> false;
    case EmbeddedProcessElement embeddedProcessElement -> false;
    case SubProcessCall subProcessCall -> false;
    default -> isElementWithMultipleIncomingFlow(element);
    };
  }

  public static List<ProcessElement> getTaskSwitchEvents(List<ProcessElement> processElements) {
    return Optional.ofNullable(processElements).orElse(Collections.emptyList()).stream()
        .filter(element -> isTaskSwitchInstance(element)).toList();
  }

  @SuppressWarnings("removal")
  public static Long getTaskStartIdFromPID(String rawPid) {
    return Ivy.session().getStartableProcessStarts().stream()
        .filter(start -> StringUtils.equals(rawPid, start.getProcessElementId())).findFirst().map(IProcessStart::getId)
        .orElse(0L);
  }

  public static String getTaskElementIdFromRequestPath(String requestPath) {
    String[] arr = requestPath.split(ProcessAnalyticsConstants.SLASH);
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
    String[] arr = requestPath.split(ProcessAnalyticsConstants.SLASH);
    return arr.length > 2 ? arr[arr.length - 2] + ProcessAnalyticsConstants.SLASH + arr[arr.length - 1]
        : StringUtils.EMPTY;
  }

  public static boolean isElementWithMultipleIncomingFlow(ProcessElement processElement) {
    return Optional.ofNullable(processElement).map(element -> element.getIncoming().size() > 1).orElse(false);
  }

  public static boolean isProcessPathEndElement(ProcessElement processElement) {
    return Optional.ofNullable(processElement).map(element -> CollectionUtils.isEmpty(element.getOutgoing()))
        .orElse(false);
  }

  public static boolean isAlternativePathEndElementWithSingleOuterFlow(ProcessElement processElement) {
    return isAlternativePathEndElement(processElement) && processElement.getOutgoing().size() == 1;
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
        String.format(ProcessAnalyticsConstants.MODULE_PATH, applicationName, selectedModule), StringUtils.EMPTY);
    int lastSlashIndex = processFilePath.lastIndexOf(ProcessAnalyticsConstants.SLASH);
    if (lastSlashIndex != StringUtils.INDEX_NOT_FOUND) {
      processFilePath = processFilePath.substring(0, lastSlashIndex) + ProcessAnalyticsConstants.PROCESSFILE_EXTENSION;
    }
    return processFilePath;
  }

  public static String buildBpmnIFrameSourceUrl(String selectedStartableId, String selectedModule) {
    IApplication application = IApplication.current();
    String processFilePath = getSelectedProcessFilePath(selectedStartableId, selectedModule, application.getName());
    String targetHost = IHtmlDialogContext.current().applicationHomeLink().toAbsoluteUri().getAuthority();
    String securityContextName = ISecurityContext.current().getName();
    if (!ISecurityContext.DEFAULT.equals(securityContextName)) {
      targetHost = StringUtils.join(targetHost, ProcessAnalyticsConstants.SLASH, securityContextName);
    }
    return String.format(ProcessAnalyticsConstants.PROCESS_ANALYSER_SOURCE_URL_PATTERN, application.getContextPath(),
        IProcessModel.current().getName(), targetHost, application.getName(), selectedModule, processFilePath);
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
