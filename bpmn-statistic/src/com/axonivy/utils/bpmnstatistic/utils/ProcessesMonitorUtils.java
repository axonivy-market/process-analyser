package com.axonivy.utils.bpmnstatistic.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.primefaces.PF;

import com.axonivy.utils.bpmnstatistic.bo.Arrow;
import com.axonivy.utils.bpmnstatistic.bo.Node;
import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;
import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;
import com.axonivy.utils.bpmnstatistic.enums.AnalysisType;
import com.axonivy.utils.bpmnstatistic.enums.IvyVariable;
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;
import com.axonivy.utils.bpmnstatistic.enums.NodeType;
import com.axonivy.utils.bpmnstatistic.repo.WorkflowProgressRepository;
import com.axonivy.utils.bpmnstatistic.service.IvyTaskOccurrenceService;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;

@SuppressWarnings("restriction")
public class ProcessesMonitorUtils {
  private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
  private static int maxArrowFrequency = 0;
  private static int maxElementFrequency = 0;

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

  public static List<Node> convertProcessElementInfoToArrows(ProcessElement element) {
    return element.getOutgoing().stream().map(flow -> convertSequenceFlowToArrow(flow)).collect(Collectors.toList());
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
  public static List<Node> filterStatisticByInterval(IProcessWebStartable processStart,
      TimeIntervalFilter timeIntervalFilter) {
    List<Node> results = new ArrayList<>();
    maxArrowFrequency = 0;
    if (Objects.nonNull(processStart)) {
      String processRawPid = ProcessUtils.getProcessPidFromElement(processStart.pid().toString());
      // Get all of element from process in 1st layer (which is not nested from sub)
      List<ProcessElement> processElements = ProcessUtils.getProcessElementsFromIProcessWebStartable(processStart);
      extractedArrowFromProcessElements(processElements, results);
      Map<String, Node> arrowMap = results.stream().collect(Collectors.toMap(Node::getId, Function.identity()));
      List<WorkflowProgress> recordedProgresses = repo.findByProcessRawPidInTime(processRawPid, timeIntervalFilter);
      recordedProgresses.stream().forEach(record -> updateArrowByWorkflowProgress(arrowMap, record));
      arrowMap.keySet().stream().forEach(key -> {
        Node currentArrowNode = arrowMap.get(key);
        currentArrowNode.setRelativeValue((float) currentArrowNode.getFrequency() / maxArrowFrequency);
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
      results.addAll(convertProcessElementInfoToArrows(element));
      if (ProcessUtils.isEmbeddedElementInstance(element)) {
        extractedArrowFromProcessElements(ProcessUtils.getNestedProcessElementsFromSub(element), results);
      }
    });
  }

  /**
   * Update info of current arrow by Workflow progress from database & update max
   * frequency of the whole element from this process.
   * 
   * @param arrow    current arrow
   * @param progress single progress instance of this arrow
   */
  private static void updateArrowByWorkflowProgress(Map<String, Node> arrowMap, WorkflowProgress record) {
    Node arrow = arrowMap.get(record.getArrowId());
    if (Objects.isNull(arrow)) {
      return;
    }
    int currentFrequency = arrow.getFrequency();
    int newFrequency = currentFrequency + 1;
    if (Objects.nonNull(record.getDuration())) {
      arrow.setMedianDuration(((arrow.getMedianDuration() * currentFrequency) + record.getDuration()) / newFrequency);
      arrow.setFrequency(newFrequency);
    }
    maxArrowFrequency = maxArrowFrequency < newFrequency ? newFrequency : maxArrowFrequency;
  }
  // private static void updateNodeByWorkflowProgress(Node node, WorkflowProgress progress) {
  //   int currentFrequency = node.getFrequency();
  //   node.setFrequency(node.getFrequency() + 1);
  //   if (Objects.nonNull(progress.getDuration())) {
  //     node.setMedianDuration(
  //         ((node.getMedianDuration() * currentFrequency) + progress.getDuration()) / (currentFrequency + 1));
  //   } else if(NodeType.ARROW == node.getType()) {
  //     node.setFrequency(node.getFrequency() - 1);
  //   }

  //   if (NodeType.ARROW == node.getType()) {
  //     maxArrowFrequency = maxArrowFrequency < currentFrequency + 1 ? currentFrequency + 1 : maxArrowFrequency;
  //   } else {
  //     maxElementFrequency = maxElementFrequency < currentFrequency + 1 ? currentFrequency + 1 : maxElementFrequency;
  //   }
  // }

  // private static void updateNodeByAnalysisType(Node node, AnalysisType analysisType) {
  //   if (AnalysisType.FREQUENCY == analysisType) {
  //     node.setLabelValue(String.valueOf(node.getRelativeValue()));
  //   } else {
  //     node.setLabelValue(String.valueOf(node.getMedianDuration()));
  //   }
  // }
}