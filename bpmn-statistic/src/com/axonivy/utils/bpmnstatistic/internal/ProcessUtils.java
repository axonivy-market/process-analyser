package com.axonivy.utils.bpmnstatistic.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.bpmnstatistic.constants.ProcessAnalyticsConstants;

import ch.ivyteam.ivy.application.IProcessModel;
import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.bpm.engine.restricted.model.IProcess;
import ch.ivyteam.ivy.bpm.engine.restricted.model.IProcessElement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.event.intermediate.TaskSwitchEvent;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.value.PID;
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

  public static boolean isTaskSwitchEvent(Object element) {
    return element instanceof TaskSwitchEvent;
  }

  public static List<ProcessElement> getNestedProcessElementsFromSub(Object element) {
    if (isEmbeddedElementInstance(element)) {
      return ((EmbeddedProcessElement) element).getEmbeddedProcess().getProcessElements();
    }
    return Collections.emptyList();
  }

  public static List<ProcessElement> getProcessElementsFromIProcessWebStartable(IProcessWebStartable startElement) {
    if (Objects.nonNull(startElement)) {
      String processRawPid = getProcessPidFromElement(startElement.pid().toString());
      var manager = IProcessManager.instance().getProjectDataModelFor(startElement.pmv());
      Process process = manager.findProcess(processRawPid, true).getModel();
      return process.getProcessElements();
    }
    return Collections.emptyList();
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
    return !(StringUtils.equals(pmName, ProcessAnalyticsConstants.BPMN_STATISTIC_PMV_NAME)
        || StringUtils.contains(pmName, ProcessAnalyticsConstants.PORTAL_PMV_SUFFIX))
        && process instanceof IProcessWebStartable;
  }

  public static List<Alternative> extractAlterNativeElementsWithMultiOutGoing(List<ProcessElement> processElements) {
    return Optional.ofNullable(processElements).orElse(Collections.emptyList()).stream()
        .filter(ProcessUtils::isAlternativeInstance).map(element -> (Alternative) element)
        .filter(alternative -> alternative.getOutgoing().size() > 1).toList();
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
}
