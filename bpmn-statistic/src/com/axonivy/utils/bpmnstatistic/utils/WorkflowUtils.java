package com.axonivy.utils.bpmnstatistic.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;

import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;
import com.axonivy.utils.bpmnstatistic.repo.WorkflowProgressRepository;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.IProcessManager;
import ch.ivyteam.ivy.process.IProjectProcessManager;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.value.PID;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

@SuppressWarnings("restriction")
public class WorkflowUtils {
	private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
	private static final int MILISECOND_IN_SECOND = 1000;

	public static void updateWorkflowInfo(String elementId) {
		Sudo.run(()->{ 
			
			Long caseId = Ivy.wf().getCurrentCase().getId();
			ITask currentTask = Ivy.wf().getCurrentTask();
			PID pid = currentTask.getStart().getProcessElementId();
			IWorkflowProcessModelVersion pmv = currentTask.getProcessModelVersion();
			ProcessElement targetElement = getProcessElementFromPmvAndPid(pmv, pid).stream()
					.filter(element -> element.getPid().toString().equalsIgnoreCase(elementId)).findAny().orElse(null);
			if (Objects.nonNull(targetElement)) {
				updateExistingWorkflowInfoForElement(targetElement.getPid().toString(), caseId);
				targetElement.getOutgoing().stream().forEach(flow -> {
					WorkflowProgress progress = new WorkflowProgress(pid.toString(), flow.getPid().toString().split("-")[1],
							targetElement.getPid().toString().split("-")[1],
							flow.getTarget().getPid().toString().split("-")[1], caseId);
					repo.save(progress);
				});
			}
		});
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

	private static List<ProcessElement> getProcessElementFromPmvAndPid(IWorkflowProcessModelVersion pmv, PID pid) {
		String processGuid = pid.getRawPid().split("-")[0];
		IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		Process process = manager.findProcess(processGuid, true).getModel();
		return process.getProcessElements();
	}

	private static void updateExistingWorkflowInfoForElement(String elementId, Long caseId) {
		elementId = elementId.split("-")[1];
		List<WorkflowProgress> oldArrows = getprocessedProcessedFlow(elementId, caseId);
		if (CollectionUtils.isEmpty(oldArrows)) {
			return;
		}
		oldArrows.stream().forEach(flow -> {
			flow.setEndTimeStamp(new Date());
			flow.setDuration(
					(flow.getEndTimeStamp().getTime() - flow.getStartTimeStamp().getTime()) / MILISECOND_IN_SECOND);
			repo.save(flow);
		});
	}

	public static Boolean isWorkflowInfoUpdatedByPidAnd(String pid, Boolean condition) {
		updateWorkflowInfo(pid);
		return condition;
	}
}
