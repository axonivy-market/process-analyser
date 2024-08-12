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
import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;
import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;
import com.axonivy.utils.bpmnstatistic.enums.IvyVariable;
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;
import com.axonivy.utils.bpmnstatistic.repo.WorkflowProgressRepository;
import com.axonivy.utils.bpmnstatistic.service.IvyTaskOccurrenceService;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;

@SuppressWarnings("restriction")
public class ProcessesMonitorUtils {
  private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
  private static int maxFrequency = 0;

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

  private static int findMaxFrequency(HashMap<String, Integer> taskCountMap) {
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
    String additionalInformation = String.format(ProcessMonitorConstants.ADDITIONAL_INFORMATION_FORMAT, instancesCount,
        fromDate, toDate);
    PF.current().executeScript(
        String.format(ProcessMonitorConstants.UPDATE_ADDITIONAL_INFORMATION_FUNCTION, additionalInformation));
  }

  public static List<Arrow> convertProcessElementInfoToArrows(ProcessElement element) {
    return element.getOutgoing().stream().map(flow -> convertSequenceFlowToArrow(flow)).collect(Collectors.toList());
  }

  private static Arrow convertSequenceFlowToArrow(SequenceFlow flow) {
    Arrow result = new Arrow();
    result.setArrowId(ProcessUtils.getElementPid(flow));
    result.setLabel(flow.getName());
    result.setFrequency(ProcessMonitorConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    result.setMedianDuration(ProcessMonitorConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    return result;
  }

  /**
   * Update the table of arrow from this process base on the current version of
   * process.
   * 
   * @param processStart selected process start
   * @return list of arrow (sequence flow) with its basic statistic data
   *         (duration, frequency)
   */
  public static List<Arrow> getStatisticData(IProcessWebStartable processStart) {
    List<Arrow> results = new ArrayList<>();
    maxFrequency = 0;
    if (Objects.nonNull(processStart)) {
      String processRawPid = ProcessUtils.getProcessPidFromElement(processStart.pid().toString());
      // Get all of element from process in 1st layer (which is not nested from sub)
      List<ProcessElement> processElements = ProcessUtils.getProcessElementsFromIProcessWebStartable(processStart);
      extractedArrowFromProcessElements(processElements, results);
      Map<String, Arrow> arrowMap = results.stream().collect(Collectors.toMap(Arrow::getArrowId, Function.identity()));
      List<WorkflowProgress> recordedProgresses = repo.findByProcessRawPid(processRawPid);
      recordedProgresses.stream().forEach(record -> updateArrowByWorkflowProgress(arrowMap, record));
      arrowMap.keySet().stream().forEach(key -> {
        Arrow currentArrow = arrowMap.get(key);
        currentArrow.setRatio((float) currentArrow.getFrequency() / maxFrequency);
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
  private static void extractedArrowFromProcessElements(List<ProcessElement> processElements, List<Arrow> results) {
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
  private static void updateArrowByWorkflowProgress(Map<String, Arrow> arrowMap, WorkflowProgress record) {
    Arrow arrow = arrowMap.get(record.getArrowId());
    if (Objects.isNull(arrow)) {
      return;
    }
    int currentFrequency = arrow.getFrequency();
    int newFrequency = currentFrequency + 1;
    if (Objects.nonNull(record.getDuration())) {
      arrow.setMedianDuration(((arrow.getMedianDuration() * currentFrequency) + record.getDuration()) / newFrequency);
      arrow.setFrequency(newFrequency);
    }
    maxFrequency = maxFrequency < newFrequency ? newFrequency : maxFrequency;
  }
}