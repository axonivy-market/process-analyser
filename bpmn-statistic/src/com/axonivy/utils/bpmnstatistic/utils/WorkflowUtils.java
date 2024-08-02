package com.axonivy.utils.bpmnstatistic.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;
import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;
import com.axonivy.utils.bpmnstatistic.repo.WorkflowProgressRepository;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.IProcessManager;
import ch.ivyteam.ivy.process.IProjectProcessManager;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

@SuppressWarnings("restriction")
public class WorkflowUtils {
  private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
  private static final int MILISECOND_IN_SECOND = 1000;

  private static void updateWorkflowInfo(String fromElementPid, Boolean conditionIsTrue, String toElementPid) {
    Long currentCaseId = ProcessUtils.getCurrentCaseId();
    String processRawPid = fromElementPid.split(ProcessMonitorConstants.HYPHEN_SIGN)[0];
    IWorkflowProcessModelVersion pmv = ProcessUtils.getCurrentTask().getProcessModelVersion();
    List<ProcessElement> processElements = getProcessElementsFromPmvAndPid(pmv, processRawPid);
    ProcessElement targetElement = isProcessElemenetPidFromSub(fromElementPid)
        ? findEmbeddedProcessEmlement(fromElementPid, processElements)
        : findTargetProcessEmlementByRawPid(fromElementPid, processElements);
    if (Objects.nonNull(targetElement)) {
      updateIncomingWorkflowInfoForElement(currentCaseId, targetElement);
      List<WorkflowProgress> outGoingWorkFlowProgress = initiateOutGoingWorkflowProgress(targetElement, currentCaseId,
          processRawPid);
      if (targetElement instanceof Alternative) {
        Predicate<WorkflowProgress> predicateByCondition = createPredicateToFilterWorkFlowProgressFromAlternativeFromCondition(
            conditionIsTrue, toElementPid);
        sanitizeWorkFlowProgressForAlternativeElement(predicateByCondition, targetElement, outGoingWorkFlowProgress);
      }
      saveNewOutGoingWorkFlowProgress(outGoingWorkFlowProgress);
    }
  }

  private static void saveNewOutGoingWorkFlowProgress(List<WorkflowProgress> outGoingWorkFlowProgress) {
    if (CollectionUtils.isNotEmpty(outGoingWorkFlowProgress)) {
      repo.save(outGoingWorkFlowProgress);
    }
  }

  private static void sanitizeWorkFlowProgressForAlternativeElement(Predicate<WorkflowProgress> predicateByCondition,
      ProcessElement targetElement, List<WorkflowProgress> outGoingWorkFlowProgress) {
    if (targetElement instanceof Alternative) {
      List<WorkflowProgress> persistedRecords = repo.findByInprogressAlternativeIdAndCaseId(
          ProcessUtils.getElementRawPid(targetElement), ProcessUtils.getCurrentCaseId());
      if (CollectionUtils.isEmpty(persistedRecords)) {
        // Keep non-failed flow only
        outGoingWorkFlowProgress.removeIf(predicateByCondition);
      } else {
        // Not save default outgoing from process if db have non-failed condition flow
        outGoingWorkFlowProgress.clear();
        persistedRecords.stream().filter(predicateByCondition).forEach(repo::delete);
      }
    }
  }

  private static ProcessElement findTargetProcessEmlementByRawPid(String fromElementPid,
      List<ProcessElement> processElements) {
    return processElements.stream().filter(element -> element.getPid().toString().equalsIgnoreCase(fromElementPid))
        .findAny().orElse(null);
  }

  private static ProcessElement findEmbeddedProcessEmlement(String fromElementPid,
      List<ProcessElement> processElements) {
    int lastHyphenIndex = fromElementPid.lastIndexOf(ProcessMonitorConstants.HYPHEN_SIGN);
    if (lastHyphenIndex == -1) {
      return null;
    }
    String subRawPid = fromElementPid.substring(0, lastHyphenIndex);
    return Optional.ofNullable((EmbeddedProcessElement) findTargetProcessEmlementByRawPid(subRawPid, processElements))
        .map(subElement -> findTargetProcessEmlementByRawPid(fromElementPid,
            subElement.getEmbeddedProcess().getProcessElements()))
        .orElse(null);
  }

  private static Predicate<WorkflowProgress> createPredicateToFilterWorkFlowProgressFromAlternativeFromCondition(
      boolean condition, String toElementPid) {
    return condition ? flow -> !isWorkFlowProgressWithTargetElementPid(flow, toElementPid)
        : flow -> isWorkFlowProgressWithTargetElementPid(flow, toElementPid);
  }

  public static void updateWorkflowInfo(String elementId) {
    updateWorkflowInfo(elementId, null, null);
  }



  private static List<ProcessElement> getProcessElementsFromPmvAndPid(IWorkflowProcessModelVersion pmv,
      String processRawPid) {
    IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(pmv);
    Process process = manager.findProcess(processRawPid, true).getModel();
    return process.getProcessElements();
  }

  private static void updateIncomingWorkflowInfoForElement(Long caseId,
      ProcessElement targetElement) {
    String elementId = ProcessUtils.getElementRawPid(targetElement);
    List<WorkflowProgress> persistedRecords = getprocessedProcessedFlow(elementId, caseId);
    if (targetElement.getParent() instanceof EmbeddedProcessElement) {
      handleWorkflowInfoForSub()
    }
    persistedRecords.stream().forEach(WorkflowUtils::updateWorkflowProgress);
  }

  private static List<WorkflowProgress> getprocessedProcessedFlow(String elementId, Long caseId) {
    int tries = 0;
    List<WorkflowProgress> results = new ArrayList<>();
    do {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Ivy.log().error(e.getMessage());
      }
      results = repo.findByTargetElementIdAndCaseId(elementId, caseId);
      if (results.size() != 0) {
        break;
      }
      tries += 1;
    } while (tries < 10);
    return results;
  }

  private static void updateWorkflowProgress(WorkflowProgress flow) {

    flow.setEndTimeStamp(new Date());
    flow.setDuration((flow.getEndTimeStamp().getTime() - flow.getStartTimeStamp().getTime()) / MILISECOND_IN_SECOND);
    flow.setDurationUpdated(true);
    repo.save(flow);
  }

  private static List<WorkflowProgress> initiateOutGoingWorkflowProgress(ProcessElement targetElement,
      long currentCaseId, String processRawPid) {
    List<WorkflowProgress> results = new ArrayList<>();
    targetElement.getOutgoing().stream().forEach(flow -> {
      WorkflowProgress progress = convertSequenceFlowToWorkFlowProgress(targetElement,
          currentCaseId,
          flow);
      results.add(progress);
//      var y = flow.getEmbeddedTarget().orElse(null);
//      Ivy.log().warn("initiateOutGoingWorkflowProgress: " + y.getConnectedOuterProcessElement());
//      Ivy.log().warn("initiateOutGoingWorkflowProgress: " + y.getConnectedOuterSequenceFlow().getPid());
//      Ivy.log().warn("initiateOutGoingWorkflowProgress: " + y.getConnectedOuterSequenceFlow().getTarget());
//      EmbeddedProcessElement x = (EmbeddedProcessElement) y.getConnectedOuterSequenceFlow().getTarget();
//      x.getEmbeddedProcess().getProcessElements().forEach(z -> {
//        Ivy.log().error(z);
//
//        if (z instanceof EmbeddedStart) {
//          Ivy.log().fatal("found roif" + z.getPid());
//          EmbeddedStart k = (EmbeddedStart) z;
//          Ivy.log().fatal(k.getConnectedOuterProcessElement());
//          Ivy.log().fatal(k.getConnectedOuterSequenceFlow().getPid());
//        }
//      });
    });
    return results;
  }

  private static WorkflowProgress convertSequenceFlowToWorkFlowProgress(ProcessElement targetElement,
      long currentCaseId, SequenceFlow flow) {
    WorkflowProgress progress = new WorkflowProgress();
    progress.setProcessRawPid(ProcessUtils.getProcessRawPidFromElement(targetElement));
    progress.setArrowId(ProcessUtils.getElementRawPid(flow));
    progress.setOriginElementId(ProcessUtils.getElementRawPid(targetElement));
    progress.setTargetElementId(ProcessUtils.getElementRawPid(flow.getTarget()));
    progress.setCaseId(currentCaseId);
    progress.setDurationUpdated(false);
    progress.setCondition(flow.getCondition());
    progress.setStartTimeStamp(new Date());
    return progress;
  }




  public static Boolean isWorkflowInfoUpdatedByPidAndAdditionalCondition(String fromElementPid, Boolean condition,
      String toElementPid) {
    updateWorkflowInfo(fromElementPid, condition, toElementPid);
    return condition;
  }


  private static boolean isWorkFlowProgressWithTargetElementPid(WorkflowProgress flow, String toElementPid) {
    String extractedTargetElementPid = StringUtils.isBlank(flow.getCondition()) ? StringUtils.EMPTY
        : extractLastQuotedContents(flow.getCondition());
    return StringUtils.equals(extractedTargetElementPid, toElementPid);
  }

  public static String extractLastQuotedContents(String input) {
    String regex = "\"([^\"]*)\"";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(input);

    List<String> contents = new ArrayList<>();

    while (matcher.find()) {
      contents.add(matcher.group(1));
    }
    return contents.size() == 0 ? StringUtils.EMPTY : contents.get(contents.size() - 1);
  }

  private static boolean isProcessElemenetPidFromSub(String rawPid) {
    if (StringUtils.isBlank(rawPid)) {
      return false;
    }
    String[] rawPidParts = rawPid.split(ProcessMonitorConstants.HYPHEN_SIGN);
    return rawPidParts.length == 3 || (rawPidParts.length > 1 && rawPidParts[rawPidParts.length - 1].contains("S"));
  }
}