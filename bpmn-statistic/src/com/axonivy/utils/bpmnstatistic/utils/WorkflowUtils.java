package com.axonivy.utils.bpmnstatistic.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;
import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;
import com.axonivy.utils.bpmnstatistic.repo.WorkflowProgressRepository;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.IProcessManager;
import ch.ivyteam.ivy.process.IProjectProcessManager;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

@SuppressWarnings("restriction")
public class WorkflowUtils {
	private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
	private static final int MILISECOND_IN_SECOND = 1000;
	private static long currentCaseId;
	private static ProcessElement targetElement;
	private static String processRawPid;

	public static void updateWorkflowInfo(String elementId) {
		processRawPid = elementId.split(ProcessMonitorConstants.HYPHEN_SIGN)[0];
		currentCaseId = getCurrentCaseId();
		IWorkflowProcessModelVersion pmv = Ivy.wf().getCurrentTask().getProcessModelVersion();
		targetElement = getProcessElementFromPmvAndPid(pmv).stream()
				.filter(element -> element.getPid().toString().equalsIgnoreCase(elementId)).findAny().orElse(null);
		if (Objects.nonNull(targetElement)) {
			updateExistingWorkflowInfoForElementWithDefinedStartElementId(elementId);
			initiateWorkflowProgressFromCurrentElement();
		}
	}

	private static long getCurrentCaseId() {
		return Sudo.get(() -> {
			return Ivy.wf().getCurrentCase().getId();
		});
	}

	private static List<ProcessElement> getProcessElementFromPmvAndPid(IWorkflowProcessModelVersion pmv) {
		IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		Process process = manager.findProcess(processRawPid, true).getModel();
		return process.getProcessElements();
	}

	private static void updateExistingWorkflowInfoForElementWithDefinedStartElementId(String elementId) {
		elementId = getElementRawId(elementId);
		List<WorkflowProgress> oldArrows = getprocessedProcessedFlow(elementId, currentCaseId);
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
		flow.setEndTimeStamp(new Date());
		flow.setDuration(
				(flow.getEndTimeStamp().getTime() - flow.getStartTimeStamp().getTime()) / MILISECOND_IN_SECOND);
		repo.save(flow);
	}

	private static void initiateWorkflowProgressFromCurrentElement() {
		targetElement.getOutgoing().stream().forEach(flow -> {
			WorkflowProgress progress = new WorkflowProgress(processRawPid, getElementRawId(flow.getPid().toString()),
					getElementRawId(targetElement.getPid().toString()),
					getElementRawId(flow.getTarget().getPid().toString()), currentCaseId);
			repo.save(progress);
		});
	}

	private static String getElementRawId(String elementid) {
		if (StringUtils.isBlank(elementid)) {
			return StringUtils.EMPTY;
		}
		int firstHyphen = elementid.indexOf(ProcessMonitorConstants.HYPHEN_SIGN);
		return elementid.substring(firstHyphen + 1);
	}

	public static Boolean isWorkflowInfoUpdatedByPidAndAdditionalCondition(String pid, Boolean condition) {
		updateWorkflowInfo(pid);
		return condition;
	}
}
