package com.axonivy.utils.bpmnstatistic.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;

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
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

@SuppressWarnings("restriction")
public class ProcessUtils {

  public static String getProcessRawPidFromElement(String targetElementId) {
    return targetElementId.split(ProcessMonitorConstants.HYPHEN_SIGN)[0];
  }

  public static String getProcessRawPidFromElement(BaseElement targetElement) {
    return getProcessRawPidFromElement(targetElement.getPid().toString());
  }

  public static String getElementRawPid(String elementid) {
    if (StringUtils.isBlank(elementid)) {
      return StringUtils.EMPTY;
    }
    int firstHyphen = elementid.indexOf(ProcessMonitorConstants.HYPHEN_SIGN);
    return elementid.substring(firstHyphen + 1);
  }

  public static String getElementRawPid(BaseElement targetElement) {
    String elementId = targetElement.getPid().toString();
    return getElementRawPid(elementId);
  }

  public static long getCurrentCaseId() {
    return Sudo.get(() -> {
      return Ivy.wf().getCurrentCase().getId();
    });
  }

  public static ITask getCurrentTask() {
    return Sudo.get(() -> {
      return Ivy.wf().getCurrentTask();
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

  public static EmbeddedEnd getEmbeddedEndFromTargetElementAndOuterFlow(ProcessElement targetElement,
      SequenceFlow flowFromEmbedded) {
    NodeElement embeddedNode = flowFromEmbedded.getSource();
    EmbeddedEnd targetEmbeddedEnds = (EmbeddedEnd) ((EmbeddedProcessElement) embeddedNode).getEmbeddedProcess()
        .getProcessElements().stream().filter(k -> k instanceof EmbeddedEnd).map(u -> (EmbeddedEnd) u)
        .filter(z -> z.getConnectedOuterProcessElement().getPid().toString().equals(targetElement.getPid().toString()))
        .findAny().orElse(null);
    return targetEmbeddedEnds;
  }

  public static List<ProcessElement> getProcessElementsFromPmvAndPid(IWorkflowProcessModelVersion pmv,
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
        .ofNullable((EmbeddedProcessElement) ProcessUtils.findTargetProcessEmlementByRawPid(subRawPid, processElements))
        .map(subElement -> ProcessUtils.findTargetProcessEmlementByRawPid(fromElementPid,
            subElement.getEmbeddedProcess().getProcessElements()))
        .orElse(null);
  }

  public static List<ProcessElement> getAllProcessElementFromCurrentTaskAndPid(String processRawPid) {
    IWorkflowProcessModelVersion pmv = ProcessUtils.getCurrentTask().getProcessModelVersion();
    IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(pmv);
    Process process = manager.findProcess(processRawPid, true).getModel();
    return process.getProcessElements();
  }
}
