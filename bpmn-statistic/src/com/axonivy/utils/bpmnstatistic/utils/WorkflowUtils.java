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

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;

@SuppressWarnings("restriction")
public class WorkflowUtils {
  private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
  private static final int MILISECOND_IN_SECOND = 1000;
  private static final String SUB_ELEMENT_PID_SUFFIX = "S";

  private static void updateWorkflowInfo(String fromElementPid, Boolean conditionIsTrue, String toElementPid) {
    fromElementPid = StringUtils.defaultString(fromElementPid, ProcessUtils.getCurrentElementPid());
    Long currentCaseId = ProcessUtils.getCurrentCaseId();
    String processRawPid = ProcessUtils.getProcessPidFromElement(fromElementPid);
    
    List<ProcessElement> processElements = ProcessUtils
        .getProcessElementsFromPmvAndProcessPid(IProcessModelVersion.current(), processRawPid);
    ProcessElement targetElement = isProcessElementNestedInSub(fromElementPid)
        ? ProcessUtils.findEmbeddedProcessElement(fromElementPid, processElements)
        : ProcessUtils.findProcessElementByRawPid(fromElementPid, processElements);
    if (Objects.nonNull(targetElement)) {
      saveTargetElement(currentCaseId, targetElement);
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

  private static void saveTargetElement(Long caseId, ProcessElement element) {
    WorkflowProgress workflowProgress = new WorkflowProgress(caseId, element.getPid().toString());
    repo.save(workflowProgress);
  }

  private static void saveNewOutGoingWorkFlowProgress(List<WorkflowProgress> outGoingWorkFlowProgress) {
    if (CollectionUtils.isNotEmpty(outGoingWorkFlowProgress)) {
      repo.save(outGoingWorkFlowProgress);
    }
  }

  /**
   * Handle case if current element is alternative:
   * 
   * + If the first option's condition is true, remove all of other option before
   * save to database
   * 
   * + If the first option's condition is false, remove it from list before save
   * other option to database. The logic will repeat for later tries.
   * 
   * @param predicateByCondition
   * @param element                  current element
   * @param outGoingWorkFlowProgress Out going flow which is ready to save into
   *                                 database
   */
  private static void sanitizeWorkFlowProgressForAlternativeElement(Predicate<WorkflowProgress> predicateByCondition,
      ProcessElement element, List<WorkflowProgress> outGoingWorkFlowProgress) {
    List<WorkflowProgress> persistedRecords = repo
        .findByInprogressAlternativeIdAndCaseId(ProcessUtils.getElementPid(element), ProcessUtils.getCurrentCaseId());
    if (CollectionUtils.isEmpty(persistedRecords)) {
      outGoingWorkFlowProgress.removeIf(predicateByCondition);
    } else {
      outGoingWorkFlowProgress.clear();
      persistedRecords.stream().filter(predicateByCondition).forEach(repo::delete);
    }
  }

  /**
   * Create predicate to detect which option do not have flag of current option
   * (from parameter) if condition is true and the opposite.
   * 
   * @param condition    condition logic from alternative option
   * @param toElementPid flag which is passed from parameter && use to detect the
   *                     current option
   * @return a logic predicated to filer non save/ delete record from database
   */
  private static Predicate<WorkflowProgress> createPredicateToFilterWorkFlowProgressFromAlternativeFromCondition(
      boolean condition, String toElementPid) {
    return condition ? flow -> !isWorkFlowProgressWithTargetElementPid(flow, toElementPid)
        : flow -> isWorkFlowProgressWithTargetElementPid(flow, toElementPid);
  }

  public static void record() {
    updateWorkflowInfo(null, null, null);
  }

  public static void record(String currentElementId) {
    updateWorkflowInfo(currentElementId, null, null);
  }

  /**
   * Update end time stamp & duration of prior workflow which connected to current
   * element. If current element is request start, skip to query data from
   * database and update it.
   * 
   * @param caseId  Current case's id
   * @param element Current element which workflow come to
   */
  private static void updateIncomingWorkflowInfoForElement(Long caseId, ProcessElement element) {
    String elementId = ProcessUtils.getElementPid(element);
    final List<WorkflowProgress> incomingFlowsToUpdate = new ArrayList<>();
    if (ProcessUtils.isRequestStartInstance(element)) {
      return;
    }
    incomingFlowsToUpdate.addAll(getSavedWorkFlowByTargetElementIdAndCaseId(elementId, caseId));
    if (CollectionUtils.isEmpty(incomingFlowsToUpdate)) {
      updateElementWithoutConnectedFlowFromDB(caseId, element, incomingFlowsToUpdate);
    }
    incomingFlowsToUpdate.stream().forEach(WorkflowUtils::updateWorkflowProgress);
  }

  /**
   * Handle exception case when workflow come to one element but can not find its
   * prior flow from database. There are 2 possible case:
   * 
   * + The current element is after process start inside sub (there is an arrow
   * with target is sub)
   * 
   * + The current element is after sub element (End process inside of sub haven't
   * been mapping with its corresponding arrow outside yet)
   * 
   * @param caseId
   * @param element          Current element which workflow come to
   * @param persistedRecords
   */
  private static void updateElementWithoutConnectedFlowFromDB(Long caseId, ProcessElement element,
      List<WorkflowProgress> persistedRecords) {
    List<SequenceFlow> incomingFlows = element.getIncoming();
    if (ProcessUtils.isEmbeddedElementInstance(element.getParent())) {
      incomingFlows.stream().forEach(flow -> persistedRecords.addAll(handleFlowFromEmbeddedElement(flow, caseId)));
    } else {
      SequenceFlow flowFromEmbedded = incomingFlows.stream()
          .filter(flow -> ProcessUtils.isEmbeddedElementInstance(flow.getSource())).findAny().orElse(null);
      persistedRecords.addAll(handleFlowOutOfEmbeddedElement(caseId, element, flowFromEmbedded));
    }
  }

  /**
   * Find embeddedEnd inside of sub to update it prior flow info && create new
   * arrow from sub to current element
   * 
   * @param caseId           Current case's id
   * @param element          Current element
   * @param flowFromEmbedded Flow between sub and current element
   * @return List of progress which include: not up-to-date flow before sub
   *         process end & new created flow from sub to current element
   */
  private static List<WorkflowProgress> handleFlowOutOfEmbeddedElement(Long caseId, NodeElement element,
      SequenceFlow flowFromEmbedded) {
    List<WorkflowProgress> persistedRecords = new ArrayList<>();
    if (Objects.isNull(flowFromEmbedded)) {
      return persistedRecords;
    }
    var targetEmbeddedEnd = ProcessUtils.getEmbeddedEndFromTargetElementAndOuterFlow(element, flowFromEmbedded);
    Optional.ofNullable(targetEmbeddedEnd).ifPresent(end -> {
      persistedRecords.addAll(getSavedInprogressWorkflowByTargetIdAndCaseId(ProcessUtils.getElementPid(end), caseId));
      WorkflowProgress workflowFromUnupdateEmbeddedStart = convertSequenceFlowToWorkFlowProgress(caseId,
          flowFromEmbedded);
      persistedRecords.add(workflowFromUnupdateEmbeddedStart);
    });
    return persistedRecords;
  }

  private static List<WorkflowProgress> getSavedWorkFlowByTargetElementIdAndCaseId(String elementId, Long caseId) {
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

  private static List<WorkflowProgress> getSavedInprogressWorkflowByTargetIdAndCaseId(String targetElementId,
      Long caseId) {
    int tries = 0;

    List<WorkflowProgress> results = new ArrayList<>();
    do {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Ivy.log().error(e.getMessage());
      }
      results = repo.findByInprogessTargetIdAndCaseId(targetElementId, caseId);
      if (results.size() != 0) {
        break;
      }
      tries += 1;
    } while (tries < 5);
    return results;
  }

  private static List<WorkflowProgress> getSavedInprogressWorkflowByArrowIdAndCaseId(String arrowId, Long caseId) {
    int tries = 0;

    List<WorkflowProgress> results = new ArrayList<>();
    do {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Ivy.log().error(e.getMessage());
      }
      results = repo.findByInprogessArrowIdAndCaseId(arrowId, caseId);
      if (results.size() != 0) {
        break;
      }
      tries += 1;
    } while (tries < 5);
    return results;
  }

  private static void updateWorkflowProgress(WorkflowProgress flow) {
    flow.setEndTimeStamp(new Date());
    flow.setDuration((flow.getEndTimeStamp().getTime() - flow.getStartTimeStamp().getTime()) / MILISECOND_IN_SECOND);
    flow.setDurationUpdated(true);
    repo.save(flow);
  }

  /**
   * Create new workflow progress for each outgoing flow from the current element
   */
  private static List<WorkflowProgress> initiateOutGoingWorkflowProgress(ProcessElement element, long currentCaseId,
      String processRawPid) {
    List<WorkflowProgress> results = new ArrayList<>();
    element.getOutgoing().stream().forEach(flow -> {
      WorkflowProgress progress = convertSequenceFlowToWorkFlowProgress(currentCaseId, flow);
      results.add(progress);
    });
    return results;
  }

  /**
   * 
   * The current flow will be check if its source is request start of sub element
   * or not. If flow source is start element, the flow outside of sub (which is
   * connect to that start) will be added to updating list. A new flow from the
   * embedded start to current element will be created and add to the return list
   * also. In case the previous element is sub, it also create new arrow between 2
   * of them and update the end flow of the prior sub.
   * 
   * @param flow   Flow going to current element
   * @param caseId Current case id
   * @return List of new WorkFlow need to be saved
   */
  private static List<WorkflowProgress> handleFlowFromEmbeddedElement(SequenceFlow flow, Long caseId) {
    SequenceFlow correspondingFlowFromOutside = ProcessUtils.getIncomingEmbeddedFlowFromStartFlow(flow);
    if (correspondingFlowFromOutside != null) {
      String outsideFlowId = ProcessUtils.getElementPid(correspondingFlowFromOutside);
      List<WorkflowProgress> persistArrow = getSavedInprogressWorkflowByArrowIdAndCaseId(outsideFlowId, caseId);
      if (CollectionUtils.isNotEmpty(persistArrow)) {
        WorkflowProgress workflowFromUnupdateEmbeddedStart = convertSequenceFlowToWorkFlowProgress(caseId, flow);
        persistArrow.add(workflowFromUnupdateEmbeddedStart);
      }
      if (ProcessUtils.isEmbeddedElementInstance(correspondingFlowFromOutside.getSource())) {
        persistArrow.addAll(handleFlowOutOfEmbeddedElement(caseId, correspondingFlowFromOutside.getTarget(),
            correspondingFlowFromOutside));
      }
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

  /**
   * An API specify for alternative option
   * 
   * @param fromElementPid the current element Id
   * @param condition      The original condition to make the process run on this
   *                       branch
   * @param toElementPid   The flag use to detect which option from alternative
   *                       (get by PMV) is running
   * @return the original condition of option
   */
  public static Boolean isWorkflowInfoUpdatedByPidAndAdditionalCondition(Boolean condition,
      String toElementPid) {
    updateWorkflowInfo(null, condition, toElementPid);
    return condition;
  }

  private static boolean isWorkFlowProgressWithTargetElementPid(WorkflowProgress flow, String toElementPid) {
    String extractedTargetElementPid = StringUtils.defaultString(extractLastQuotedContents(flow.getCondition()));
    return StringUtils.equals(extractedTargetElementPid, toElementPid);
  }

  /**
   * Extract the detecting flag from alternative option's condition
   * 
   * @param input String of condition
   * @return the flag in type String
   */
  public static String extractLastQuotedContents(String input) {
    Pattern pattern = Pattern.compile(ProcessMonitorConstants.QUOTED_CONTENT_PATTERN);
    Matcher matcher = pattern.matcher(input);
    List<String> contents = new ArrayList<>();
    while (matcher.find()) {
      contents.add(matcher.group(1));
    }
    return contents.size() == 0 ? StringUtils.EMPTY : contents.get(contents.size() - 1);
  }

  private static boolean isProcessElementNestedInSub(String rawPid) {
    if (StringUtils.isBlank(rawPid)) {
      return false;
    }
    String[] rawPidParts = rawPid.split(ProcessMonitorConstants.HYPHEN_SIGN);
    return rawPidParts.length == 3
        || (rawPidParts.length > 1 && rawPidParts[rawPidParts.length - 1].contains(SUB_ELEMENT_PID_SUFFIX));
  }
}