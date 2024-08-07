package com.axonivy.utils.bpmnstatistic.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
    result.setFrequency(ProcessMonitorConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    result.setMedianDuration(ProcessMonitorConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    return result;
  }

  public static List<Arrow> getStatisticData(IProcessWebStartable processStart) {
    List<Arrow> results = new ArrayList<>();
    maxFrequency = 0;
    Map<String, Arrow> arrowMap = new HashMap<String, Arrow>();
    if (Objects.nonNull(processStart)) {
      String processRawPid = ProcessUtils.getProcessPidFromElement(processStart.pid().toString());
      extractedArrowFromProcessStart(processStart, results);
      results.stream().forEach(arrow -> arrowMap.put(arrow.getArrowId(), arrow));
      List<WorkflowProgress> recordedProgresses = repo.findByProcessRawPid(processRawPid);
      recordedProgresses.stream()
          .forEach(record -> updateArrowByWorkflowProgress(arrowMap.get(record.getArrowId()), record));
      arrowMap.keySet().stream().forEach(key -> {
        Arrow currentArrow = arrowMap.get(key);
        currentArrow.setRatio((float) currentArrow.getFrequency() / maxFrequency);
      });
    }
    return results;
  }

  private static void extractedArrowFromProcessStart(IProcessWebStartable processStart, List<Arrow> results) {
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFromIProcessWebStartable(processStart);
    List<ProcessElement> additionalProcessElements = new ArrayList<ProcessElement>();
    processElements.stream().filter(ProcessUtils::isEmbeddedElementInstance)
        .forEach(element -> additionalProcessElements.addAll(ProcessUtils.getProcessElementFromSub(element)));
    processElements.addAll(additionalProcessElements);
    processElements.forEach(element -> results.addAll(convertProcessElementInfoToArrows(element)));
  }

  private static int updateArrowByWorkflowProgress(Arrow arrow, WorkflowProgress progress) {
    int currentFrequency = arrow.getFrequency();
    if (Objects.nonNull(progress.getDuration())) {
      arrow.setMedianDuration(
          ((arrow.getMedianDuration() * currentFrequency) + progress.getDuration()) / (currentFrequency + 1));
      arrow.setFrequency(arrow.getFrequency() + 1);
    }
    return maxFrequency = maxFrequency < currentFrequency + 1 ? currentFrequency + 1 : maxFrequency;
  }
}