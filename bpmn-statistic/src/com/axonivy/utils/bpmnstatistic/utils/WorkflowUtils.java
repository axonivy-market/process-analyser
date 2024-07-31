package com.axonivy.utils.bpmnstatistic.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

@SuppressWarnings("restriction")
public class WorkflowUtils {
  private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
  private static final int MILISECOND_IN_SECOND = 1000;

  private static void updateWorkflowInfo(String fromElementPid, Boolean conditionIsTrue, String toElementPid) {
    Long currentCaseId = getCurrentCaseId();
    String processRawPid = fromElementPid.split(ProcessMonitorConstants.HYPHEN_SIGN)[0];
    IWorkflowProcessModelVersion pmv = getCurrentTask().getProcessModelVersion();
    ProcessElement targetElement = getProcessElementFromPmvAndPid(pmv, processRawPid).stream()
        .filter(element -> element.getPid().toString().equalsIgnoreCase(fromElementPid)).findAny().orElse(null);
    if (Objects.isNull(targetElement)) {
      return;
    }
    updateIncomingWorkflowInfoForElement(fromElementPid, currentCaseId);

    // Initiate default outgoing from process
    List<WorkflowProgress> outGoingWorkFlowProgress = initiateOutGoingWorkflowProgress(targetElement, currentCaseId,
        processRawPid);

    if (targetElement instanceof Alternative) {

      // Looking for workflow record which is non-failed
      List<WorkflowProgress> persit = repo
          .findByInprogressAlternativeIdAndCaseId(getElementRawId(targetElement.getPid().toString()), currentCaseId);

      if (CollectionUtils.isEmpty(persit)) {

        // Keep non-failed flow only
        handleNewAlternativeWorkFlow(conditionIsTrue, toElementPid, outGoingWorkFlowProgress);
      } else {
        // Not save default outgoing from process if db have non-failed condition flow
        outGoingWorkFlowProgress = new ArrayList<WorkflowProgress>();
        updateRecoredWorkflow(conditionIsTrue, toElementPid, persit);
      }
    }
    if (CollectionUtils.isNotEmpty(outGoingWorkFlowProgress)) {
      repo.save(outGoingWorkFlowProgress);
    }
  }

  private static List<WorkflowProgress> handleNewAlternativeWorkFlow(Boolean conditionIsTrue, String toElementPid,
      List<WorkflowProgress> outGoingWorkFlowProgress) {
    if (conditionIsTrue) {
      outGoingWorkFlowProgress.removeIf(flow -> !isWorkFlowProgressWithTargetElementPid(flow, toElementPid));

    } else {
      outGoingWorkFlowProgress.removeIf(flow -> isWorkFlowProgressWithTargetElementPid(flow, toElementPid));
    }
    return outGoingWorkFlowProgress;
  }

  private static void updateRecoredWorkflow(Boolean conditionIsTrue, String toElementPid,
      List<WorkflowProgress> persit) {

    if (conditionIsTrue) {
      persit.stream().filter(flow -> !isWorkFlowProgressWithTargetElementPid(flow, toElementPid)).forEach(repo::delete);
    } else {
      persit.stream().filter(flow -> isWorkFlowProgressWithTargetElementPid(flow, toElementPid)).forEach(repo::delete);
    }
  }

  public static void updateWorkflowInfo(String elementId) {
    updateWorkflowInfo(elementId, null, null);
  }

  private static long getCurrentCaseId() {
    return Sudo.get(() -> {
      return Ivy.wf().getCurrentCase().getId();
    });
  }

  private static ITask getCurrentTask() {
    return Sudo.get(() -> {
      return Ivy.wf().getCurrentTask();
    });
  }

  private static List<ProcessElement> getProcessElementFromPmvAndPid(IWorkflowProcessModelVersion pmv,
      String processRawPid) {
    IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(pmv);
    Process process = manager.findProcess(processRawPid, true).getModel();
    return process.getProcessElements();
  }

  private static void updateIncomingWorkflowInfoForElement(String elementId, Long caseId) {
    elementId = getElementRawId(elementId);
    List<WorkflowProgress> oldArrows = getprocessedProcessedFlow(elementId, caseId);
    if (CollectionUtils.isEmpty(oldArrows)) {
      return;
    }
    oldArrows.stream().forEach(WorkflowUtils::updateWorkflowProgress);

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
    if (flow.isFromInProgessAlternativeOrigin()) {
      flow.setFromInProgessAlternativeOrigin(false);
    }
    flow.setEndTimeStamp(new Date());
    flow.setDuration((flow.getEndTimeStamp().getTime() - flow.getStartTimeStamp().getTime()) / MILISECOND_IN_SECOND);
    repo.save(flow);
  }

  private static List<WorkflowProgress> initiateOutGoingWorkflowProgress(ProcessElement targetElement,
      long currentCaseId, String processRawPid) {
    List<WorkflowProgress> results = new ArrayList<>();
    targetElement.getOutgoing().stream().forEach(flow -> {
      WorkflowProgress progress = new WorkflowProgress();
      progress.setProcessRawPid(processRawPid);
      progress.setArrowId(getElementRawId(flow.getPid().toString()));
      progress.setOriginElementId(getElementRawId(targetElement.getPid().toString()));
      progress.setTargetElementId(getElementRawId(flow.getTarget().getPid().toString()));
      progress.setCaseId(currentCaseId);
      progress.setFromInProgessAlternativeOrigin(targetElement instanceof Alternative);
      progress.setCondition(flow.getCondition());
      progress.setStartTimeStamp(new Date());
      results.add(progress);
      Ivy.log().warn("initiateOutGoingWorkflowProgress: " + progress.isFromInProgessAlternativeOrigin());
    });
    return results;
  }

  private static String getElementRawId(String elementid) {
    if (StringUtils.isBlank(elementid)) {
      return StringUtils.EMPTY;
    }
    int firstHyphen = elementid.indexOf(ProcessMonitorConstants.HYPHEN_SIGN);
    return elementid.substring(firstHyphen + 1);
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
}
