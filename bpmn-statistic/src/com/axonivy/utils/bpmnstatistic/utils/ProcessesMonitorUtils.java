package com.axonivy.utils.bpmnstatistic.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.PF;

import com.axonivy.utils.bpmnstatistic.bo.Node;
import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;
import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;
import com.axonivy.utils.bpmnstatistic.enums.AnalysisType;
import com.axonivy.utils.bpmnstatistic.enums.IvyVariable;
import com.axonivy.utils.bpmnstatistic.enums.NodeType;
import com.axonivy.utils.bpmnstatistic.repo.WorkflowProgressRepository;
import com.axonivy.utils.bpmnstatistic.service.IvyTaskOccurrenceService;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.IProcessManager;
import ch.ivyteam.ivy.process.IProjectProcessManager;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

@SuppressWarnings("restriction")
public class ProcessesMonitorUtils {

  public static final String BPMN_STATISTIC_PMV = "bpmn-statistic";

  private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
  private static int maxArrowFrequency = 0;
  private static int maxElementFrequency = 0;

  private ProcessesMonitorUtils() {};

  public static List<IWebStartable> getAllProcesses() {
    return Ivy.session().getStartables().stream().filter(process -> isNotPortalHomeAndMSTeamsProcess(process))
        .filter(process -> !process.pmv().getName().equals(BPMN_STATISTIC_PMV)).collect(Collectors.toList());
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
    return !StringUtils.endsWithAny(relativeEncoded, ProcessMonitorConstants.PORTAL_START_REQUEST_PATH,
        ProcessMonitorConstants.PORTAL_IN_TEAMS_REQUEST_PATH);
  }

  public static void showStatisticData(String pid) {
    Objects.requireNonNull(pid);
    HashMap<String, Integer> taskCountMap = IvyTaskOccurrenceService.countTaskOccurrencesByProcessId(pid);
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
    return String.valueOf(
        Ivy.var().get(String.format(ProcessMonitorConstants.FREQUENCY_BACKGROUND_COLOR_LEVEL_VARIABLE_PATTERN, level)));
  }

  public static void showAdditionalInformation(String instancesCount, String fromDate, String toDate) {
    String additionalInformation =
        String.format(ProcessMonitorConstants.ADDITIONAL_INFORMATION_FORMAT, instancesCount, fromDate, toDate);
    PF.current().executeScript(
        String.format(ProcessMonitorConstants.UPDATE_ADDITIONAL_INFORMATION_FUNCTION, additionalInformation));
  }

  private static List<ProcessElement> getProcessElementFromPmvAndPid(IWorkflowProcessModelVersion pmv, String pid) {
    IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(pmv);
    Process process = manager.findProcess(pid, true).getModel();
    return process.getProcessElements();
  }

  public static List<Node> convertProcessElementInfoToArrows(ProcessElement element) {
    return element.getOutgoing().stream().map(flow -> convertSequenceFlowToArrow(flow)).collect(Collectors.toList());
  }

  private static Node convertSequenceFlowToArrow(SequenceFlow flow) {
    Node arrowNode = new Node();
    arrowNode.setId(flow.getPid().toString().split(ProcessMonitorConstants.HYPHEN_SIGN)[1]);
    arrowNode.setLabel(flow.getName());
    arrowNode.setRelativeValue(ProcessMonitorConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    arrowNode.setMedianDuration(ProcessMonitorConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    arrowNode.setType(NodeType.ARROW);
    return arrowNode;
  }

  public static List<Node> getStatisticData(IProcessWebStartable processStart, AnalysisType analysisType) {
    List<Node> nodes = new ArrayList<>();
    maxArrowFrequency = 0;
    maxElementFrequency = 0;
    Map<String, Node> nodeMap = new HashMap<String, Node>();
    if (Objects.nonNull(processStart)) {
      String processRawPid = processStart.pid().toString().split(ProcessMonitorConstants.HYPHEN_SIGN)[0];
      List<ProcessElement> processElements =
          getProcessElementFromPmvAndPid((IWorkflowProcessModelVersion) processStart.pmv(), processRawPid);
      processElements.forEach(element -> {
        Node node = new Node();
        node.setId(element.getPid().toString().split(ProcessMonitorConstants.HYPHEN_SIGN)[1]);
        node.setType(NodeType.ELEMENT);
        node.setLabel(element.getName());
        nodes.add(node);
        nodes.addAll(convertProcessElementInfoToArrows(element));
      });
      nodes.stream().forEach(node -> nodeMap.put(node.getId(), node));
      List<WorkflowProgress> recordedProgresses = repo.findByProcessRawPid(processRawPid);
      recordedProgresses.stream()
          .forEach(record -> updateNodeByWorkflowProgress(nodeMap.get(record.getElementId()), record));
      nodeMap.keySet().stream().forEach(key -> {
        Node currentNode = nodeMap.get(key);
        currentNode.setRelativeValue((float) currentNode.getFrequency()
            / (NodeType.ARROW == currentNode.getType() ? maxArrowFrequency : maxElementFrequency));
        updateNodeByAnalysisType(currentNode, analysisType);
      });
    }
    return nodes;
  }

  private static void updateNodeByWorkflowProgress(Node node, WorkflowProgress progress) {
    int currentFrequency = node.getFrequency();
    node.setFrequency(node.getFrequency() + 1);
    if (Objects.nonNull(progress.getDuration())) {
      node.setMedianDuration(
          ((node.getMedianDuration() * currentFrequency) + progress.getDuration()) / (currentFrequency + 1));
    } else if(NodeType.ARROW == node.getType()) {
      node.setFrequency(node.getFrequency() - 1);
    }

    if (NodeType.ARROW == node.getType()) {
      maxArrowFrequency = maxArrowFrequency < currentFrequency + 1 ? currentFrequency + 1 : maxArrowFrequency;
    } else {
      maxElementFrequency = maxElementFrequency < currentFrequency + 1 ? currentFrequency + 1 : maxElementFrequency;
    }
  }

  private static void updateNodeByAnalysisType(Node node, AnalysisType analysisType) {
    if (AnalysisType.FREQUENCY == analysisType) {
      node.setLabelValue(String.valueOf(node.getRelativeValue()));
    } else {
      node.setLabelValue(String.valueOf(node.getMedianDuration()));
    }
  }
}
