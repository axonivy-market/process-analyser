package com.axonivy.utils.bpmnstatistic.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.IProcessManager;
import ch.ivyteam.ivy.process.IProjectProcessManager;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.event.end.EmbeddedEnd;
import ch.ivyteam.ivy.process.model.element.event.start.EmbeddedStart;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@SuppressWarnings("restriction")
public class ProcessUtils {
  private ProcessUtils() {
  }

  public static String getElementPid(BaseElement baseElement) {
    return Optional.ofNullable(baseElement).map(element -> element.getPid().toString()).orElse(StringUtils.EMPTY);
  }

  public static String getProcessPidFromElement(String elementId) {
    return StringUtils.defaultString(elementId).split(ProcessMonitorConstants.HYPHEN_SIGN)[0];
  }

  public static String getProcessRawPidFromElement(BaseElement baseElement) {
    String elementId = getElementPid(baseElement);
    return getProcessPidFromElement(elementId);
  }
//
//  public static String getElementRawPid(String elementId) {
//    if (StringUtils.isBlank(elementId)) {
//      return StringUtils.EMPTY;
//    }
//    int firstHyphen = elementId.indexOf(ProcessMonitorConstants.HYPHEN_SIGN);
//    return elementId.substring(firstHyphen + 1);
//  }
//
//  public static String getElementRawPid(BaseElement baseElement) {
//    String elementId = getElementPid(baseElement);
//    return getElementRawPid(elementId);
//  }

  public static long getCurrentCaseId() {
    return Sudo.get(() -> {
      return Ivy.wfCase().getId();
    });
  }

  public static ITask getCurrentTask() {
    return Sudo.get(() -> {
      return Ivy.wfTask();
    });
  }

  public static boolean isEmbeddedElementInstance(Object element) {
    return element instanceof EmbeddedProcessElement;
  }

  public static boolean isAlternativeInstance(Object element) {
    return element instanceof Alternative;
  }

  public static List<ProcessElement> getProcessElementFromSub(Object element) {
    if (isEmbeddedElementInstance(element)) {
      return ((EmbeddedProcessElement) element).getEmbeddedProcess().getProcessElements();
    }
    return Collections.emptyList();
  }

  public static String getIncomingEmbeddedFlowFromStartFlow(SequenceFlow flow) {
    NodeElement sourceElement = flow.getSource();
    if (sourceElement instanceof EmbeddedStart) {
      EmbeddedStart embeddedStart = (EmbeddedStart) sourceElement;
      return embeddedStart.getConnectedOuterSequenceFlow().getPid().toString();
    }
    return StringUtils.EMPTY;
  }

  public static EmbeddedEnd getEmbeddedEndFromTargetElementAndOuterFlow(ProcessElement processElement,
      SequenceFlow flowFromEmbedded) {
    EmbeddedProcessElement embeddedNode = (EmbeddedProcessElement) flowFromEmbedded.getSource();
    EmbeddedEnd targetEmbeddedEnds = (EmbeddedEnd) embeddedNode.getEmbeddedProcess()
        .getProcessElements().stream().filter(ProcessUtils::isEmbeddedEnd)
        .map(EmbeddedEnd.class::cast)
        .filter(embeddedEnd -> isConnectedToProcessElement(embeddedEnd, processElement))
        .findAny().orElse(null);
    return targetEmbeddedEnds;
  }

  private static boolean isEmbeddedEnd(ProcessElement element) {
    return element instanceof EmbeddedEnd;
  }

  private static boolean isConnectedToProcessElement(EmbeddedEnd embeddedEnd, ProcessElement processElement) {
    String connectedElementPid = getElementPid(embeddedEnd.getConnectedOuterProcessElement());
    String processElementPid = getElementPid(processElement);
    return StringUtils.equals(connectedElementPid, processElementPid);
  }

  public static List<ProcessElement> getProcessElementsFromPmvAndProcessPid(IProcessModelVersion pmv,
      String processRawPid) {
    IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(pmv);
    Process process = manager.findProcess(processRawPid, true).getModel();
    return process.getProcessElements();
  }

  public static ProcessElement findTargetProcessEmlementByRawPid(String fromElementPid,
      List<ProcessElement> processElements) {
    return processElements.stream().filter(element -> element.getPid().toString().equalsIgnoreCase(fromElementPid))
        .findAny().orElse(null);
  }

  public static ProcessElement findEmbeddedProcessEmlement(String fromElementPid,
      List<ProcessElement> processElements) {
    int lastHyphenIndex = fromElementPid.lastIndexOf(ProcessMonitorConstants.HYPHEN_SIGN);
    if (lastHyphenIndex == -1) {
      return null;
    }
    String subRawPid = fromElementPid.substring(0, lastHyphenIndex);
    return Optional
        .ofNullable((EmbeddedProcessElement) findTargetProcessEmlementByRawPid(subRawPid, processElements))
        .map(subElement -> findTargetProcessEmlementByRawPid(fromElementPid,
            subElement.getEmbeddedProcess().getProcessElements()))
        .orElse(null);
  }

  /** This method is used for runtime case **/
  public static List<ProcessElement> getProcessElementsFromCurrentTaskAndProcessPid(String processRawPid) {
    IProcessModelVersion pmv = getCurrentTask().getProcessModelVersion();
    return getProcessElementsFromPmvAndProcessPid(pmv, processRawPid);
  }

  public static List<ProcessElement> getProcessElementsFromIProcessWebStartable(IProcessWebStartable startElement) {
    if (Objects.nonNull(startElement)) {
      String processRawPid = getProcessPidFromElement(startElement.pid().toString());
      return getProcessElementsFromPmvAndProcessPid(startElement.pmv(), processRawPid);
    }
    return Collections.emptyList();
  }

  public static List<IWebStartable> getAllProcesses() {
    return Ivy.session().getStartables().stream().filter(ProcessUtils::isIWebStartableNeedToRecordStatistic)
        .toList();
  }

  public static Map<String, List<IProcessWebStartable>> getProcessesWithPmv() {
    Map<String, List<IProcessWebStartable>> result = new HashMap<>();
    for (IWebStartable process : getAllProcesses()) {
      String pmvName = process.pmv().getName();
      result.computeIfAbsent(pmvName, key -> new ArrayList<>()).add((IProcessWebStartable) process);
    }
    return result;
  }

  private static boolean isIWebStartableNeedToRecordStatistic(IWebStartable process) {
    return !(StringUtils.equals(process.pmv().getName(), ProcessMonitorConstants.BPMN_STATISTIC_PMV)
        || StringUtils.contains(process.pmv().getName(), "portal"));
  }

}
