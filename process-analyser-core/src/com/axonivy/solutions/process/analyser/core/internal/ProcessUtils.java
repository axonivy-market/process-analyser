package com.axonivy.solutions.process.analyser.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.event.intermediate.TaskSwitchEvent;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.Join;
import ch.ivyteam.ivy.process.model.value.PID;
import ch.ivyteam.ivy.process.rdm.IProcess;
import ch.ivyteam.ivy.process.rdm.IProcessManager;
import ch.ivyteam.ivy.workflow.IProcessStart;
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
    return element instanceof EmbeddedProcessElement;
  }

  public static boolean isAlternativeInstance(Object element) {
    return element instanceof Alternative;
  }

  public static boolean isTaskSwitchInstance(Object element) {
    return element instanceof TaskSwitchEvent;
  }

  public static boolean isTaskJoinInstance(Object element) {
    return element instanceof Join;
  }

  public static List<ProcessElement> getNestedProcessElementsFromSub(Object element) {
    if (isEmbeddedElementInstance(element)) {
      return ((EmbeddedProcessElement) element).getEmbeddedProcess().getProcessElements();
    }
    return Collections.emptyList();
  }

  public static List<ProcessElement> getProcessElementsFrom(IProcessWebStartable startElement) {
    if (startElement == null || startElement.pid() == null) {
      return Collections.emptyList();
    }

    String processRawPid = getProcessPidFromElement(startElement.pid().toString());
    var manager = IProcessManager.instance().getProjectDataModelFor(startElement.pmv());

    IProcess foundProcess = manager.findProcess(processRawPid, true);
    if (foundProcess == null) {
      return Collections.emptyList();
    }

    // Get all process elements, including nested ones
    return foundProcess.getModel().getProcessElements().stream().flatMap(
        element -> Stream.concat(Stream.of(element), ProcessUtils.getNestedProcessElementsFromSub(element).stream()))
        .collect(Collectors.toList());
  }

  public static List<SequenceFlow> getSequenceFlowsFrom(List<ProcessElement> elements) {
    return elements.stream().flatMap(element -> element.getOutgoing().stream())
        .collect(Collectors.toList());
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
        && process instanceof IProcessWebStartable;
  }

  public static List<ProcessElement> getAlterNativesWithMultiOutgoings(List<ProcessElement> processElements) {
    return Optional.ofNullable(processElements).orElse(new ArrayList<>()).stream()
        .filter(element -> isAlternativeInstance(element) && element.getOutgoing().size() > 1)
        .collect(Collectors.toList());
  }

  public static List<ProcessElement> getElementsWithMultiIncomings(List<ProcessElement> processElements) {
    return Optional.ofNullable(processElements).orElse(new ArrayList<>()).stream()
        .filter(element -> !(isAlternativeInstance(element) && isTaskJoinInstance(processElements))
            && isElementWithMultipleIncomingFlow(element))
        .collect(Collectors.toList());
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

  public static boolean isElementWithMultipleIncomingFlow(ProcessElement processElement) {
    return Optional.ofNullable(processElement).map(element -> element.getIncoming().size() > 1).orElse(false);
  }

  public static boolean isProcessPathEndElement(ProcessElement processElement) {
    return Optional.ofNullable(processElement).map(element -> CollectionUtils.isEmpty(element.getOutgoing()))
        .orElse(false);
  }

  public static boolean isAlternativePathEndElement(ProcessElement processElement) {
    return isProcessPathEndElement(processElement) || isAlternativeInstance(processElement)
        || (isElementWithMultipleIncomingFlow(processElement) && !isTaskJoinInstance(processElement));
  }
}
