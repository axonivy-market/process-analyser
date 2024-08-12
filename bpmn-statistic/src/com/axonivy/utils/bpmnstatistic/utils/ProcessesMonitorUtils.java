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

import com.axonivy.utils.bpmnstatistic.bo.Arrow;
import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;
import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;
import com.axonivy.utils.bpmnstatistic.enums.IvyVariable;
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

  public static final String BPMN_STATISTIC_PMV ="bpmn-statistic";

  private static final WorkflowProgressRepository repo = WorkflowProgressRepository.getInstance();
  private static int maxFrequency = 0;

  private ProcessesMonitorUtils() {
  };

  public static List<IWebStartable> getAllProcesses() {
    return Ivy.session().getStartables().stream().filter(process -> isNotPortalHomeAndMSTeamsProcess(process))
        .filter(process -> !process.pmv().getName().equals(BPMN_STATISTIC_PMV))
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

  private static List<ProcessElement> getProcessElementFromPmvAndPid(IWorkflowProcessModelVersion pmv, String pid) {
    IProjectProcessManager manager = IProcessManager.instance().getProjectDataModelFor(pmv);
    Process process = manager.findProcess(pid, true).getModel();
    return process.getProcessElements();
  }

  public static List<Arrow> convertProcessElementInfoToArrows(ProcessElement element) {
    return element.getOutgoing().stream().map(flow -> convertSequenceFlowToArrow(flow)).collect(Collectors.toList());
  }

  private static Arrow convertSequenceFlowToArrow(SequenceFlow flow) {
    Arrow result = new Arrow();
    result.setArrowId(flow.getPid().toString().split(ProcessMonitorConstants.HYPHEN_SIGN)[1]);
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
      String processRawPid = processStart.pid().toString().split(ProcessMonitorConstants.HYPHEN_SIGN)[0];
      List<ProcessElement> processElements = getProcessElementFromPmvAndPid(
          (IWorkflowProcessModelVersion) processStart.pmv(), processRawPid);
      processElements.forEach(element -> results.addAll(convertProcessElementInfoToArrows(element)));
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

  private static int updateArrowByWorkflowProgress(Arrow arrow, WorkflowProgress progress) {
    if (arrow == null) {
      return maxFrequency;
    }
    int currentFrequency = arrow.getFrequency();
    if (Objects.nonNull(progress.getDuration())) {
      arrow.setMedianDuration(
          ((arrow.getMedianDuration() * currentFrequency) + progress.getDuration()) / (currentFrequency + 1));
      arrow.setFrequency(arrow.getFrequency() + 1);
    }
    return maxFrequency = maxFrequency < currentFrequency + 1 ? currentFrequency + 1 : maxFrequency;
  }
  
  public static List<Arrow> filterStatisticByInterval(IProcessWebStartable processStart, TimeIntervalFilter timeIntervalFilter) {
    // TODO Filter data here
    List<Arrow> results = new ArrayList<>();
    maxFrequency = 0;
    Map<String, Arrow> arrowMap = new HashMap<String, Arrow>();
    if (Objects.nonNull(processStart)) {
      String processRawPid = processStart.pid().toString().split(ProcessMonitorConstants.HYPHEN_SIGN)[0];
      List<ProcessElement> processElements = getProcessElementFromPmvAndPid(
          (IWorkflowProcessModelVersion) processStart.pmv(), processRawPid);
      processElements.forEach(element -> results.addAll(convertProcessElementInfoToArrows(element)));
      results.stream().forEach(arrow -> arrowMap.put(arrow.getArrowId(), arrow));
      // TODO new code
      List<WorkflowProgress> recordedProgresses = repo.findByProcessRawPidInTime(processRawPid, timeIntervalFilter);
      recordedProgresses.stream()
          .forEach(record -> updateArrowByWorkflowProgress(arrowMap.get(record.getArrowId()), record));
      arrowMap.keySet().stream().forEach(key -> {
        Arrow currentArrow = arrowMap.get(key);
        currentArrow.setRatio((float) currentArrow.getFrequency() / maxFrequency);
      });
    }
    return results;
  }
}