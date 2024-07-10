package com.axonivy.utils.bpmnstatistic.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PF;

import com.axonivy.utils.bpmnstatistic.bo.Arrow;
import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;
import com.axonivy.utils.bpmnstatistic.enums.IvyVariable;
import com.axonivy.utils.bpmnstatistic.repo.WorkflowProgressRepository;
import com.axonivy.utils.bpmnstatistic.service.IvyTaskOccurrenceService;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.IProcessManager;
import ch.ivyteam.ivy.process.IProjectProcessManager;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.value.PID;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@SuppressWarnings("restriction")
public class ProcessesMonitorUtils {
	private static final String PORTAL_START_REQUEST_PATH = "/DefaultApplicationHomePage.ivp";
	private static final String PORTAL_IN_TEAMS_REQUEST_PATH = "InTeams.ivp";
	private static final String REMOVE_DEFAULT_HIGHLIGHT_JS_FUNCTION = "santizeDiagram();";
	private static final String UPDATE_FREQUENCY_COUNT_FOR_TASK_FUNCTION = "addElementFrequency('%s', '%s', '%s', '%s');";
	private static final String FREQUENCY_BACKGROUND_COLOR_LEVEL_VARIABLE_PATTERN = "frequencyBackgroundColorLevel%s";
	private static final int DEFAULT_BACKGROUND_COLOR_LEVEL = 1;
	private static final int HIGHEST_LEVEL_OF_BACKGROUND_COLOR = 6;
	private static final String ADDIATION_INFORMATION_FORMAT = "%s instances (investigation period:%s - %s)";
	private static final String UPDATE_ADDITION_INFORMATION_FUNCTION = "updateAdditionalInformation('%s')";
	private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
	private static final int DEFAULT_INITIAL_NUMBER = 0;
	private static final int MILISECOND_IN_SECOND = 1000;
	private static int maxFrequency = 0;

	private ProcessesMonitorUtils() {
	};

	public static List<IWebStartable> getAllProcesses() {
		return Ivy.session().getStartables().stream().filter(process -> isNotPortalHomeAndMSTeamsProcess(process))
				.collect(Collectors.toList());
	}

	public static Map<String, List<IProcessWebStartable>> getProcessesWithPmv() {
		Map<String, List<IProcessWebStartable>> result = new HashMap<>();
		for (IWebStartable process : getAllProcesses()) {
			String pmvName = process.pmv().getName();
			result.computeIfAbsent(pmvName, key -> new ArrayList<>()).add((IProcessWebStartable) process);
		}
		return result;
	}

	private static boolean isNotPortalHomeAndMSTeamsProcess(IWebStartable process) {

		String relativeEncoded = process.getLink().getRelativeEncoded();
		return !StringUtils.endsWithAny(relativeEncoded, PORTAL_START_REQUEST_PATH, PORTAL_IN_TEAMS_REQUEST_PATH);
	}

	public static void showStatisticData(String pid) {
		Objects.requireNonNull(pid);
		HashMap<String, Integer> taskCountMap = IvyTaskOccurrenceService.countTaskOccurrencesByProcessId(pid);
		int maxFrequency = findMaxFrequency(taskCountMap);
		String textColorRGBCode = String.valueOf(Ivy.var().get(IvyVariable.FREQUENCY_NUMBER_COLOR.getVariableName()));
		PF.current().executeScript(REMOVE_DEFAULT_HIGHLIGHT_JS_FUNCTION);
		for (Entry<String, Integer> entry : taskCountMap.entrySet()) {
			String backgroundColorRGBCode = getRGBCodefromFrequency(maxFrequency, entry.getValue());
			PF.current().executeScript(String.format(UPDATE_FREQUENCY_COUNT_FOR_TASK_FUNCTION, entry.getKey(),
					entry.getValue(), backgroundColorRGBCode, textColorRGBCode));
		}
	}

	private static int findMaxFrequency(HashMap<String, Integer> taskCountMap) {
		int max = 0;
		for (Entry<String, Integer> entry : taskCountMap.entrySet()) {
			max = max < entry.getValue() ? entry.getValue() : max;
		}
		return max;
	}

	private static String getRGBCodefromFrequency(int max, int current) {
		int level = (int) (max == 0 ? DEFAULT_BACKGROUND_COLOR_LEVEL
				: Math.ceil(current * HIGHEST_LEVEL_OF_BACKGROUND_COLOR / max));
		return String.valueOf(Ivy.var().get(String.format(FREQUENCY_BACKGROUND_COLOR_LEVEL_VARIABLE_PATTERN, level)));
	}

	public void showAdditionalInformation(String instancesCount, String fromDate, String toDate) {
		String additionalInformation = String.format(ADDIATION_INFORMATION_FORMAT, instancesCount, fromDate, toDate);
		PF.current().executeScript(String.format(UPDATE_ADDITION_INFORMATION_FUNCTION, additionalInformation));
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
					(flow.getEndTimeStamp().getTime()- flow.getStartTimeStamp().getTime())/MILISECOND_IN_SECOND);
			repo.save(flow);
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

	public static void updateWorkflowInfo(String elementId) {
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
	}

	private static List<ProcessElement> getProcessElementFromPmvAndPid(IWorkflowProcessModelVersion pmv, PID pid) {
		String processGuid = pid.getRawPid().split("-")[0];
		IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		Process process = manager.findProcess(processGuid, true).getModel();
		return process.getProcessElements();
	}

	public static List<Arrow> convertProcessElementInfoToArrows(ProcessElement element) {
		return element.getOutgoing().stream().map(flow -> convertSequenceFlowToArrow(flow))
				.collect(Collectors.toList());
	}

	private static Arrow convertSequenceFlowToArrow(SequenceFlow flow) {
		Arrow result = new Arrow();
		result.setArrowId(flow.getPid().toString().split("-")[1]);
		result.setLabel(flow.getName());
		result.setFrequency(DEFAULT_INITIAL_NUMBER);
		result.setFrequency(DEFAULT_INITIAL_NUMBER);
		result.setMedianDuration(DEFAULT_INITIAL_NUMBER);
		return result;
	}

	public static List<Arrow> getStatisticData(IProcessWebStartable processStart) {
		List<Arrow> results = new ArrayList<>();
		maxFrequency = 0;
		Map<String, Arrow> arrowMap = new HashMap<String, Arrow>();
		if (Objects.nonNull(processStart)) {
			PID pid = processStart.pid();
			List<ProcessElement> processElements = getProcessElementFromPmvAndPid(
					(IWorkflowProcessModelVersion) processStart.pmv(), pid);
			processElements.forEach(element -> results.addAll(convertProcessElementInfoToArrows(element)));
			results.stream().forEach(arrow -> arrowMap.put(arrow.getArrowId(), arrow));
			List<WorkflowProgress> recordedProgresses = repo.findByProcessRawPid(pid.toString());
			recordedProgresses.stream()
					.forEach(record -> updateArrowByWorkflowProgress(arrowMap.get(record.getArrowId()), record));
			arrowMap.keySet().stream().forEach(key -> {
				Arrow currentArrow = arrowMap.get(key);
				currentArrow.setRatio((float) currentArrow.getFrequency() / maxFrequency);
			});
		}
		return results;
	}

	private static int updateArrowByWorkflowProgress(Arrow arrow, WorkflowProgress progress) {
		int currentFrequency = arrow.getFrequency();
		arrow.setMedianDuration(((arrow.getMedianDuration() * currentFrequency) + progress.getDuration())
				/ (currentFrequency + 1));
		arrow.setFrequency(arrow.getFrequency() + 1);
		return maxFrequency = maxFrequency < currentFrequency + 1 ? currentFrequency + 1 : maxFrequency;
	}
}