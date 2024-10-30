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

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.bpm.engine.restricted.model.IProcessElement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.event.end.EmbeddedEnd;
import ch.ivyteam.ivy.process.model.element.event.intermediate.TaskSwitchEvent;
import ch.ivyteam.ivy.process.model.element.event.start.EmbeddedStart;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.value.PID;
import ch.ivyteam.ivy.process.rdm.IProcessManager;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.restricted.start.CaseMapWebStartable;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@SuppressWarnings("restriction")
public class ProcessUtils {
  private ProcessUtils() {
  }

  public static String getElementPid(BaseElement baseElement) {
    return Optional.ofNullable(baseElement).map(BaseElement::getPid).map(PID::toString).orElse(StringUtils.EMPTY);
  }

  public static String getElementPid(PID pid) {
    return Optional.ofNullable(pid).map(PID::toString).orElse(StringUtils.EMPTY);
  }

  public static String getProcessPidFromElement(String elementId) {
    return StringUtils.defaultString(elementId).split(ProcessAnalyticsConstants.HYPHEN_SIGN)[0];
  }

  public static String getProcessRawPidFromElement(BaseElement baseElement) {
    String elementId = getElementPid(baseElement);
    return getProcessPidFromElement(elementId);
  }

  public static long getCurrentCaseId() {
    return Sudo.get(() -> {
      return Ivy.wfCase().getId();
    });
  }

  public static boolean isEmbeddedElementInstance(Object element) {
    return element instanceof EmbeddedProcessElement;
  }

  public static boolean isAlternativeInstance(Object element) {
    return element instanceof Alternative;
  }

  public static boolean isRequestStartInstance(Object element) {
    return element instanceof RequestStart;
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

  /**
   * Get sequence flow from outside of sub (EmbeddedProcessElement) which is prior
   * of the target flow
   * 
   * @param flow Sequence flow of which origin (tail) is embedded start
   * @return Pid of sequence flow which is connected to target embedded start from
   *         outside of sub
   */
  public static SequenceFlow getIncomingEmbeddedFlowFromStartFlow(SequenceFlow flow) {
    NodeElement sourceElement = flow.getSource();
    if (sourceElement instanceof EmbeddedStart) {
      EmbeddedStart embeddedStart = (EmbeddedStart) sourceElement;
      return embeddedStart.getConnectedOuterSequenceFlow();
    }
    return null;
  }

  public static String getIncomingEmbeddedFlowIdFromStartFlow(SequenceFlow flow) {
    NodeElement sourceElement = flow.getSource();
    if (sourceElement instanceof EmbeddedStart) {
      EmbeddedStart embeddedStart = (EmbeddedStart) sourceElement;
      return embeddedStart.getConnectedOuterSequenceFlow().getPid().toString();
    }
    return StringUtils.EMPTY;
  }

  /**
   * Get end embedded process element inside the sub (EmbeddedProcessElement)
   * which connected to the current element and it incoming flow.
   * 
   * @param processElement   Element that sub connected to
   * @param flowFromEmbedded flow outside of sub which connected to current
   *                         processElement
   * @return Embedded process end from inside of sub
   */
  public static EmbeddedEnd getEmbeddedEndFromTargetElementAndOuterFlow(NodeElement processElement,
      SequenceFlow flowFromEmbedded) {
    EmbeddedProcessElement embeddedNode = (EmbeddedProcessElement) flowFromEmbedded.getSource();
    EmbeddedEnd targetEmbeddedEnds = embeddedNode.getEmbeddedProcess().getProcessElements().stream()
        .filter(ProcessUtils::isEmbeddedEnd).map(EmbeddedEnd.class::cast)
        .filter(embeddedEnd -> isConnectedToProcessElement(embeddedEnd, processElement)).findAny().orElse(null);
    return targetEmbeddedEnds;
  }

  private static boolean isEmbeddedEnd(ProcessElement element) {
    return element instanceof EmbeddedEnd;
  }

  private static boolean isConnectedToProcessElement(EmbeddedEnd embeddedEnd, NodeElement processElement) {
    String connectedElementPid = getElementPid(embeddedEnd.getConnectedOuterProcessElement());
    String processElementPid = getElementPid(processElement);
    return StringUtils.equals(connectedElementPid, processElementPid);
  }

  public static List<ProcessElement> getProcessElementsFromPmvAndProcessPid(IProcessModelVersion pmv,
      String processRawPid) {
    var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
    Process process = manager.findProcess(processRawPid, true).getModel();
    return process.getProcessElements();
  }

  public static ProcessElement findProcessElementByRawPid(String fromElementPid, List<ProcessElement> processElements) {
    return processElements.stream().filter(element -> element.getPid().toString().equalsIgnoreCase(fromElementPid))
        .findAny().orElse(null);
  }

  public static ProcessElement findEmbeddedProcessElement(String fromElementPid, List<ProcessElement> processElements) {
    int lastHyphenIndex = fromElementPid.lastIndexOf(ProcessAnalyticsConstants.HYPHEN_SIGN);
    if (lastHyphenIndex == -1) {
      return null;
    }
    String subRawPid = fromElementPid.substring(0, lastHyphenIndex);
    return Optional.ofNullable((EmbeddedProcessElement) findProcessElementByRawPid(subRawPid, processElements)).map(
        subElement -> findProcessElementByRawPid(fromElementPid, subElement.getEmbeddedProcess().getProcessElements()))
        .orElse(null);
  }

  public static List<ProcessElement> getProcessElementsFromIProcessWebStartable(IProcessWebStartable startElement) {
    if (Objects.nonNull(startElement)) {
      String processRawPid = getProcessPidFromElement(startElement.pid().toString());
      return getProcessElementsFromPmvAndProcessPid(startElement.pmv(), processRawPid);
    }
    return Collections.emptyList();
  }

  public static List<IWebStartable> getAllProcesses() {
    return Ivy.session().getStartables().stream().filter(ProcessUtils::isIWebStartableNeedToRecordStatistic).toList();
  }

  public static Map<String, List<IProcessWebStartable>> getProcessesWithPmv() {
    Map<String, List<IProcessWebStartable>> result = new HashMap<>();
    for (IWebStartable process : getAllProcesses()) {
      if (process instanceof CaseMapWebStartable) {
        continue;
      }
      String pmvName = process.pmv().getProcessModel().getName();
      result.computeIfAbsent(pmvName, key -> new ArrayList<>()).add((IProcessWebStartable) process);
    }
    return result;
  }

  private static boolean isIWebStartableNeedToRecordStatistic(IWebStartable process) {
    var pmName = process.pmv().getProcessModel().getName();
    return !(StringUtils.equals(pmName, ProcessAnalyticsConstants.BPMN_STATISTIC_PMV_NAME)
        || StringUtils.contains(pmName, ProcessAnalyticsConstants.PORTAL_PMV_SUFFIX));
  }

  public static boolean isContainFlowFromSubElement(List<SequenceFlow> flows) {
    return flows.stream().anyMatch(flow -> ProcessUtils.isEmbeddedElementInstance(flow.getSource()));
  }

  public static String getCurrentElementPid() {
    PID currentElementPid = Optional.ofNullable(IProcessElement.current()).map(IProcessElement::getId).orElse(null);
    return getElementPid(currentElementPid);
  }

  public static List<Alternative> extractAlterNativeElementsWithMultiOutGoing(List<ProcessElement> processElements) {
    return Optional.ofNullable(processElements).orElse(Collections.emptyList()).stream()
        .filter(ProcessUtils::isAlternativeInstance).map(element -> (Alternative) element)
        .filter(alternative -> alternative.getOutgoing().size() > 1).toList();
  }
}
