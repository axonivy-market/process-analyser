package com.axonivy.utils.bpmnstatistic.utils;

import java.util.ArrayList;
import java.util.Collections;
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
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;
import com.axonivy.utils.bpmnstatistic.repo.WorkflowProgressRepository;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;

@SuppressWarnings("restriction")
public class WorkflowUtils {
  private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
  private static final int MILISECOND_IN_SECOND = 1000;

  private static void updateWorkflowInfo(String fromElementPid, Boolean conditionIsTrue, String toElementPid) {
    Long currentCaseId = ProcessUtils.getCurrentCaseId();
    String processRawPid = ProcessUtils.getProcessPidFromElement(fromElementPid);
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFromCurrentTaskAndProcessPid(processRawPid);
    ProcessElement targetElement = isProcessElemenetPidFromSub(fromElementPid)
        ? ProcessUtils.findEmbeddedProcessEmlement(fromElementPid, processElements)
        : ProcessUtils.findTargetProcessEmlementByRawPid(fromElementPid, processElements);
    if (Objects.nonNull(targetElement)) {
      updateIncomingWorkflowInfoForElement(currentCaseId, targetElement);
      List<WorkflowProgress> outGoingWorkFlowProgress = initiateOutGoingWorkflowProgress(targetElement, currentCaseId,
          processRawPid);
      if (ProcessUtils.isAlternativeInstance(targetElement)) {
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
    if (ProcessUtils.isAlternativeInstance(targetElement)) {
      List<WorkflowProgress> persistedRecords = repo.findByInprogressAlternativeIdAndCaseId(
          ProcessUtils.getElementPid(targetElement), ProcessUtils.getCurrentCaseId());
      if (CollectionUtils.isEmpty(persistedRecords)) {
        outGoingWorkFlowProgress.removeIf(predicateByCondition);
      } else {
        outGoingWorkFlowProgress.clear();
        persistedRecords.stream().filter(predicateByCondition).forEach(repo::delete);
      }
    }
  }

  private static Predicate<WorkflowProgress> createPredicateToFilterWorkFlowProgressFromAlternativeFromCondition(
      boolean condition, String toElementPid) {
    return condition ? flow -> !isWorkFlowProgressWithTargetElementPid(flow, toElementPid)
        : flow -> isWorkFlowProgressWithTargetElementPid(flow, toElementPid);
  }

  public static void updateWorkflowInfo(String elementId) {
    updateWorkflowInfo(elementId, null, null);
  }

  private static void updateIncomingWorkflowInfoForElement(Long caseId, ProcessElement targetElement) {
    String elementId = ProcessUtils.getElementPid(targetElement);
    List<WorkflowProgress> persistedRecords = getprocessedProcessedFlow(elementId, caseId);
    if (CollectionUtils.isEmpty(persistedRecords)) {
      updateElementWithoutConnectedFlow(caseId, targetElement, persistedRecords);
    }
    persistedRecords.stream().forEach(WorkflowUtils::updateWorkflowProgress);
  }

  private static void updateElementWithoutConnectedFlow(Long caseId, ProcessElement targetElement,
      List<WorkflowProgress> persistedRecords) {
    List<SequenceFlow> incomingFlow = targetElement.getIncoming();
    if (ProcessUtils.isEmbeddedElementInstance(targetElement.getParent())) {
      incomingFlow.stream().forEach(flow -> persistedRecords.addAll(handleFlowFromEmbeddedElement(flow, caseId)));
    } else {
      SequenceFlow flowFromEmbedded = incomingFlow.stream()
          .filter(flow -> ProcessUtils.isEmbeddedElementInstance(flow.getSource())).findAny().orElse(null);
      persistedRecords.addAll(handleFlowOutOfEmbeddedElement(caseId, targetElement, flowFromEmbedded));
    }
  }

  private static List<WorkflowProgress> handleFlowOutOfEmbeddedElement(Long caseId, ProcessElement targetElement,
      SequenceFlow flowFromEmbedded) {
    List<WorkflowProgress> persistedRecords = new ArrayList<>();
    if (Objects.isNull(flowFromEmbedded)) {
      return persistedRecords;
    }
    var targetEmbeddedEnd = ProcessUtils.getEmbeddedEndFromTargetElementAndOuterFlow(targetElement, flowFromEmbedded);
    Optional.ofNullable(targetEmbeddedEnd).ifPresent(end -> {
      persistedRecords.addAll(repo.findByInprogessTargetIdAndCaseId(ProcessUtils.getElementPid(end), caseId));
      WorkflowProgress workflowFromUnupdateEmbeddedStart = convertSequenceFlowToWorkFlowProgress(caseId,
          flowFromEmbedded);
      persistedRecords.add(workflowFromUnupdateEmbeddedStart);
    });
    return persistedRecords;
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
      WorkflowProgress progress = convertSequenceFlowToWorkFlowProgress(currentCaseId, flow);
      results.add(progress);
    });
    return results;
  }

  private static List<WorkflowProgress> handleFlowFromEmbeddedElement(SequenceFlow flow, Long caseId) {
    String correspondingFlowIdFromOutside = ProcessUtils.getIncomingEmbeddedFlowFromStartFlow(flow);
    List<WorkflowProgress> persistArrow = repo
        .findByInprogessArrowIdAndCaseId(correspondingFlowIdFromOutside, caseId);
    if (CollectionUtils.isNotEmpty(persistArrow)) {
      WorkflowProgress workflowFromUnupdateEmbeddedStart = convertSequenceFlowToWorkFlowProgress(caseId, flow);
      persistArrow.add(workflowFromUnupdateEmbeddedStart);
      return persistArrow;
    }
    return Collections.emptyList();
  }

  private static WorkflowProgress convertSequenceFlowToWorkFlowProgress(long currentCaseId, SequenceFlow flow) {
    WorkflowProgress progress = new WorkflowProgress();
    progress.setProcessRawPid(ProcessUtils.getProcessRawPidFromElement(flow));
    progress.setArrowId(ProcessUtils.getElementPid(flow));
    progress.setOriginElementId(ProcessUtils.getElementPid(flow.getSource()));
    progress.setTargetElementId(ProcessUtils.getElementPid(flow.getTarget()));
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
    Pattern pattern = Pattern.compile(ProcessMonitorConstants.QUOTED_CONTENT_PATTERN);
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