package com.axonivy.utils.bpmnstatistic.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PF;

import com.axonivy.utils.bpmnstatistic.bo.AlternativePath;
import com.axonivy.utils.bpmnstatistic.bo.Node;
import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;
import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;
import com.axonivy.utils.bpmnstatistic.enums.AnalysisType;
import com.axonivy.utils.bpmnstatistic.enums.IvyVariable;
import com.axonivy.utils.bpmnstatistic.enums.NodeType;
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;
import com.axonivy.utils.bpmnstatistic.repo.WorkflowProgressRepository;
import com.axonivy.utils.bpmnstatistic.service.IvyTaskOccurrenceService;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.event.intermediate.TaskSwitchEvent;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.workflow.CaseState;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.query.CaseQuery;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;

@SuppressWarnings("restriction")
public class ProcessesMonitorUtils {
	private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
	private static int maxArrowFrequency = 0;
	private static int maxElementFrequency = 0;
	private static final String SLASH = "/";

	private ProcessesMonitorUtils() {
	}

	/**
	 * Get more additional insight from process viewer (task count, number of
	 * instances with interval time range,...) when user click "show statistic
	 * data".
	 * 
	 * @param pid rawPid of selected process
	 */
	public static void showStatisticData(String pid) {
		Objects.requireNonNull(pid);
		HashMap<String, Integer> taskCountMap = IvyTaskOccurrenceService.countTaskOccurrencesByProcessId(pid);
		Ivy.log().warn(taskCountMap);
		int maxFrequency = findMaxFrequency(taskCountMap);
		String textColorRGBCode = String.valueOf(Ivy.var().get(IvyVariable.FREQUENCY_NUMBER_COLOR.getVariableName()));
		PF.current().executeScript(ProcessMonitorConstants.REMOVE_DEFAULT_HIGHLIGHT_JS_FUNCTION);
		for (Entry<String, Integer> entry : taskCountMap.entrySet()) {
			String backgroundColorRGBCode = getRGBCodefromFrequency(maxFrequency, entry.getValue());
			PF.current().executeScript(String.format(ProcessMonitorConstants.UPDATE_FREQUENCY_COUNT_FOR_TASK_FUNCTION,
					entry.getKey(), entry.getValue(), backgroundColorRGBCode, textColorRGBCode));
		}
	}

	public static int findMaxFrequency(HashMap<String, Integer> taskCountMap) {
		int max = 0;
		for (Entry<String, Integer> entry : taskCountMap.entrySet()) {
			max = max < entry.getValue() ? entry.getValue() : max;
		}
		return max;
	}

	private static String getRGBCodefromFrequency(int max, int current) {
		int level = (int) (max == 0 ? ProcessMonitorConstants.DEFAULT_BACKGROUND_COLOR_LEVEL
				: Math.ceil(current * ProcessMonitorConstants.HIGHEST_LEVEL_OF_BACKGROUND_COLOR / max));
		return String.valueOf(Ivy.var()
				.get(String.format(ProcessMonitorConstants.FREQUENCY_BACKGROUND_COLOR_LEVEL_VARIABLE_PATTERN, level)));
	}

	public static void showAdditionalInformation(String instancesCount, String fromDate, String toDate) {
		String additionalInformation = String.format(ProcessMonitorConstants.ADDITIONAL_INFORMATION_FORMAT,
				instancesCount, fromDate, toDate);
		PF.current().executeScript(
				String.format(ProcessMonitorConstants.UPDATE_ADDITIONAL_INFORMATION_FUNCTION, additionalInformation));
	}

	public static List<Node> convertProcessElementInfoToArrows(ProcessElement element) {
		return element.getOutgoing().stream().map(flow -> convertSequenceFlowToArrow(flow))
				.collect(Collectors.toList());
	}

	private static Node convertSequenceFlowToArrow(SequenceFlow flow) {
		Node arrowNode = new Node();
		arrowNode.setId(ProcessUtils.getElementPid(flow));
		arrowNode.setLabel(flow.getName());
		arrowNode.setRelativeValue(ProcessMonitorConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
		arrowNode.setMedianDuration(ProcessMonitorConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
		arrowNode.setType(NodeType.ARROW);
		return arrowNode;
	}

	/**
	 * Update the table of arrow from this process base on the current version of
	 * process.
	 * 
	 * @param processStart       selected process start
	 * @param timeIntervalFilter selected time interval
	 * @return list of arrow (sequence flow) with its basic statistic data
	 *         (duration, frequency)
	 */
	public static List<Node> filterInitialStatisticByInterval(IProcessWebStartable processStart,
			TimeIntervalFilter timeIntervalFilter, AnalysisType analysisType) {
		List<Node> results = new ArrayList<>();
		maxArrowFrequency = 0;
		if (Objects.nonNull(processStart)) {
			String processRawPid = ProcessUtils.getProcessPidFromElement(processStart.pid().toString());
			// Get all of element from process in 1st layer (which is not nested from sub)
			List<ProcessElement> processElements = ProcessUtils
					.getProcessElementsFromIProcessWebStartable(processStart);
			extractedArrowFromProcessElements(processElements, results);
			Map<String, Node> nodeMap = results.stream().collect(Collectors.toMap(Node::getId, Function.identity()));
			List<WorkflowProgress> recordedProgresses = repo.findByProcessRawPidInTime(processRawPid,
					timeIntervalFilter);
			recordedProgresses.stream().forEach(record -> updateElementOrArrowByWorkflowProgress(nodeMap, record));
			nodeMap.keySet().stream().forEach(key -> {
				Node node = nodeMap.get(key);
				node.setRelativeValue((float) node.getFrequency() / maxArrowFrequency);
				updateNodeByAnalysisType(node, analysisType);
			});
		}
		return results;
	}

	/**
	 * Get outgoing arrows from each element. If the current element is sub
	 * (Embedded element), it will get all of nested element and execute the same
	 * thing until all of sub is extracted.
	 * 
	 * @param processElements list of process elements need to get its out going
	 *                        workflow
	 * @param results         list of existing arrow from previous step
	 */
	private static void extractedArrowFromProcessElements(List<ProcessElement> processElements, List<Node> results) {
		processElements.forEach(element -> {
			results.add(convertProcessElementToNode(element));
			results.addAll(convertProcessElementInfoToArrows(element));
			if (ProcessUtils.isEmbeddedElementInstance(element)) {
				extractedArrowFromProcessElements(ProcessUtils.getNestedProcessElementsFromSub(element), results);
			}
		});
	}

	private static Node convertProcessElementToNode(ProcessElement element) {
		Node elementNode = new Node();
		elementNode.setId(element.getPid().toString());
		elementNode.setLabel(element.getName());
		elementNode.setRelativeValue(ProcessMonitorConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
		elementNode.setMedianDuration(ProcessMonitorConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
		elementNode.setType(NodeType.ELEMENT);
		return elementNode;
	}

	/**
	 * Update info of current arrow by Workflow progress from database & update max
	 * frequency of the whole element from this process.
	 * 
	 * @param arrow    current arrow
	 * @param progress single progress instance of this arrow
	 */
	private static void updateElementOrArrowByWorkflowProgress(Map<String, Node> nodeMap, WorkflowProgress record) {
		if (StringUtils.isBlank(record.getArrowId())) {
			updateElementByWorkflowProgress(nodeMap, record);
		} else {
			updateArrowByWorkflowProgress(nodeMap, record);
		}
	}

	private static void updateElementByWorkflowProgress(Map<String, Node> nodeMap, WorkflowProgress record) {
		Node element = nodeMap.get(record.getProcessRawPid());
		if (Objects.isNull(element)) {
			return;
		}
		int newFrequency = element.getFrequency() + 1;
		element.setFrequency(newFrequency);
		maxElementFrequency = maxElementFrequency < newFrequency ? newFrequency : maxElementFrequency;
	}

	private static void updateArrowByWorkflowProgress(Map<String, Node> nodeMap, WorkflowProgress record) {
		Node arrow = nodeMap.get(record.getArrowId());
		if (Objects.isNull(arrow)) {
			return;
		}
		int currentFrequency = arrow.getFrequency();
		int newFrequency = currentFrequency + 1;
		if (Objects.nonNull(record.getDuration())) {
			arrow.setMedianDuration(
					((arrow.getMedianDuration() * currentFrequency) + record.getDuration()) / newFrequency);
			arrow.setFrequency(newFrequency);
		}
		maxArrowFrequency = maxArrowFrequency < newFrequency ? newFrequency : maxArrowFrequency;
	}

	private static void updateNodeByAnalysisType(Node node, AnalysisType analysisType) {
		if (AnalysisType.FREQUENCY == analysisType) {
			node.setLabelValue(String.valueOf(node.getRelativeValue()));
		} else {
			node.setLabelValue(String.valueOf(node.getMedianDuration()));
		}
	}

	public static List<Node> filterStatisticByInterval(IProcessWebStartable processStart,
			TimeIntervalFilter timeIntervalFilter, AnalysisType analysisType) {
		List<Node> results = new ArrayList<>();
		maxArrowFrequency = 0;
		if (Objects.nonNull(processStart)) {
			String processRawPid = ProcessUtils.getProcessPidFromElement(processStart.pid().toString());
			// Get all of element from process in 1st layer (which is not nested from sub)
			List<ProcessElement> processElements = ProcessUtils
					.getProcessElementsFromIProcessWebStartable(processStart);
			extractedArrowFromProcessElements(processElements, results);
			Map<String, Node> nodeMap = results.stream().collect(Collectors.toMap(Node::getId, Function.identity()));
			List<WorkflowProgress> recordedProgresses = repo.findByProcessRawPidInTime(processRawPid,
					timeIntervalFilter);
			recordedProgresses.stream().forEach(record -> updateElementOrArrowByWorkflowProgress(nodeMap, record));
			nodeMap.keySet().stream().forEach(key -> {
				Node node = nodeMap.get(key);
				node.setRelativeValue((float) node.getFrequency() / maxArrowFrequency);
				updateNodeByAnalysisType(node, analysisType);
			});
		}
		return results;
	}

	// TODO: New Approach - please handle from here

	public static List<Node> newApproach(IProcessWebStartable processStart, TimeIntervalFilter timeIntervalFilter,
			AnalysisType analysisType) {
		if (Objects.isNull(processStart)) {
			return Collections.emptyList();
		}
		maxArrowFrequency = 0;
		List<Node> results = new ArrayList<>();
		Long taskStartId = WorkflowUtils.getTaskStartIdFromPID(processStart.pid().toString());
		String processRawPid = ProcessUtils.getProcessPidFromElement(processStart.pid().toString());
		List<ProcessElement> processElements = ProcessUtils.getProcessElementsFromIProcessWebStartable(processStart);
		extractedArrowFromProcessElements(processElements, results);
		List<Alternative> alterNativeElement = ProcessUtils
				.extractAlterNativeElementsWithMultiOutGoing(processElements);
		List<ICase> cases = getAllCasesFromTaskStartId(taskStartId);
		extractedArrowFromProcessElements(processElements, results);
		return updateFrequencyWithAlternative(results, alterNativeElement, cases);

	}

	// If the current case is not contain any alternative, total count = number of
	// case for all element. If the current case contain any alternative, only count
	// for running path
	private static List<Node> updateFrequencyWithAlternative(List<Node> results, List<Alternative> alternatives,
			List<ICase> cases) {
		if (CollectionUtils.isEmpty(alternatives)) {
			results.stream().forEach(node -> updateDefinedDataForNode(cases.size(), node));
		} else {
			alternatives.stream()
					.forEach(alternative -> handleFrequencyForAlternativePath(alternative, results, cases));
		}
		return results;
	}

	private static void handleFrequencyForAlternativePath(Alternative alternative, List<Node> results,
			List<ICase> cases) {
		List<AlternativePath> paths = new ArrayList<>();
		Ivy.log().warn("alternative" + alternative.getPid());
		List<SequenceFlow> mainFlowFromAlternative = alternative.getOutgoing();
		mainFlowFromAlternative.forEach(path -> {
			AlternativePath currentPath = new AlternativePath();
			currentPath.setOriginFlow(path);
			currentPath.setNodeIdsInPath(new ArrayList<>());
			followPath(currentPath, path);
			paths.add(currentPath);
		});
		updateFrequencyForCaseWithSimpleAlternative(paths, results, cases);
	}

	private static void updateFrequencyForCaseWithSimpleAlternative(List<AlternativePath> paths, List<Node> results,
			List<ICase> cases) {
		List<String> nonRunningNode =  new ArrayList<>();
		cases.stream().forEach(currentCase -> {
			List<String> taskIdDoneInCase = currentCase.tasks().all().stream()
					.map(iTask -> WorkflowUtils.getTaskElementIdFromRequestPath(iTask.getRequestPath())).toList();
			Ivy.log().warn("ftaskIdDoneInCase " + taskIdDoneInCase);
			AlternativePath runningPath = paths.stream()
					.filter(path -> taskIdDoneInCase.contains(path.getTaskSwitchEventIdOnPath())).findAny()
					.orElse(paths.stream().filter(path -> StringUtils.isBlank(path.getTaskSwitchEventIdOnPath())).findAny().orElse(null));
			Ivy.log().warn(runningPath);
			paths.remove(runningPath);
			paths.stream().forEach(path -> nonRunningNode.addAll(path.getNodeIdsInPath()));
			// TODO: update frequency for right object
			results.stream().filter(node -> !nonRunningNode.contains(node.getId())).forEach(node -> {
				node.setFrequency(node.getFrequency() +1);
			});
		});
	}

	// Follow the path to collect all of elements & task switch event in current
	// path. Stop following when reaching the joint point
	private static void followPath(AlternativePath path, SequenceFlow currentFlow) {
		ProcessElement destinationElement = (ProcessElement) currentFlow.getTarget();
		path.getNodeIdsInPath().add(ProcessUtils.getElementPid(destinationElement));
		path.getNodeIdsInPath().add(ProcessUtils.getElementPid(currentFlow));
		if (ProcessUtils.isTaskSwitchEvent(destinationElement)) {
			path.setTaskSwitchEventIdOnPath(ProcessUtils.getElementPid(destinationElement));
		}
		if (destinationElement.getIncoming().size() > 1) {
			path.setEndProcessElementId(ProcessUtils.getElementPid(destinationElement));
		}
		destinationElement.getOutgoing().forEach(outGoingPath -> followPath(path, outGoingPath));
	}

	private static List<ICase> getAllCasesFromTaskStartId(Long taskStartId) {
		return CaseQuery.create().where().state().isEqual(CaseState.DONE).and().taskStartId().isEqual(taskStartId)
				.executor().results();
	}

	// Update default value for node
	private static void updateDefinedDataForNode(int value, Node node) {
		node.setRelativeValue(1L);
		node.setLabelValue(Optional.of(value).orElse(null).toString());
		node.setFrequency(value);
	}

}