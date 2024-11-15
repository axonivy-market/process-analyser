package com.axonivy.utils.bpmnstatistic.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PF;

import com.axonivy.utils.bpmnstatistic.bo.AlternativePath;
import com.axonivy.utils.bpmnstatistic.bo.Node;
import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.constants.ProcessAnalyticsConstants;
import com.axonivy.utils.bpmnstatistic.enums.KpiType;
import com.axonivy.utils.bpmnstatistic.enums.IvyVariable;
import com.axonivy.utils.bpmnstatistic.enums.NodeType;
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;
import com.axonivy.utils.bpmnstatistic.service.IvyTaskOccurrenceService;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.workflow.CaseState;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.custom.field.CustomFieldType;
import ch.ivyteam.ivy.workflow.custom.field.ICustomFieldMeta;
import ch.ivyteam.ivy.workflow.query.CaseQuery;
import ch.ivyteam.ivy.workflow.query.TaskQuery;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;

@SuppressWarnings("restriction")
public class ProcessesMonitorUtils {
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
    PF.current().executeScript(ProcessAnalyticsConstants.REMOVE_DEFAULT_HIGHLIGHT_JS_FUNCTION);
    for (Entry<String, Integer> entry : taskCountMap.entrySet()) {
      String backgroundColorRGBCode = getRGBCodefromFrequency(maxFrequency, entry.getValue());
      PF.current().executeScript(String.format(ProcessAnalyticsConstants.UPDATE_FREQUENCY_COUNT_FOR_TASK_FUNCTION,
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
    int level = (int) (max == 0 ? ProcessAnalyticsConstants.DEFAULT_BACKGROUND_COLOR_LEVEL
        : Math.ceil(current * ProcessAnalyticsConstants.HIGHEST_LEVEL_OF_BACKGROUND_COLOR / max));
    return String.valueOf(
        Ivy.var().get(String.format(ProcessAnalyticsConstants.FREQUENCY_BACKGROUND_COLOR_LEVEL_VARIABLE_PATTERN, level)));
  }

  public static void showAdditionalInformation(String instancesCount, String fromDate, String toDate) {
    String additionalInformation = String.format(ProcessAnalyticsConstants.ADDITIONAL_INFORMATION_FORMAT, instancesCount,
        fromDate, toDate);
    PF.current().executeScript(
        String.format(ProcessAnalyticsConstants.UPDATE_ADDITIONAL_INFORMATION_FUNCTION, additionalInformation));
  }

  public static List<Node> convertProcessElementInfoToArrows(ProcessElement element) {
    return element.getOutgoing().stream().map(flow -> convertSequenceFlowToArrow(flow)).collect(Collectors.toList());
  }

  private static Node convertSequenceFlowToArrow(SequenceFlow flow) {
    Node arrowNode = new Node();
    arrowNode.setId(ProcessUtils.getElementPid(flow));
    arrowNode.setLabel(flow.getName());
    arrowNode.setFrequency(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    arrowNode.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    arrowNode.setMedianDuration(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    arrowNode.setType(NodeType.ARROW);
    return arrowNode;
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
    elementNode.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    elementNode.setFrequency(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    elementNode.setMedianDuration(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    elementNode.setType(NodeType.ELEMENT);
    return elementNode;
  }


  private static void updateNodeByAnalysisType(Node node, KpiType analysisType) {
    if (KpiType.FREQUENCY == analysisType) {
      node.setLabelValue(node.getFrequency());
    } else {
      node.setLabelValue((int)Math.round(node.getMedianDuration()));
    }
    if (Double.isNaN(node.getRelativeValue())) {
      node.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    }
  }



  /**
   * New approach to show bpmn statistic data without modifying original process.
   * All of material which is use to analyzing will be based on task data from
   * AxonIvy system db.
   **/
  public static List<Node> filterInitialStatisticByIntervalWithoutModifyingProcess(IProcessWebStartable processStart,
      TimeIntervalFilter timeIntervalFilter, KpiType analysisType, Map<ICustomFieldMeta, Object> selectedCustomFilters) {
    if (Objects.isNull(processStart)) {
      return Collections.emptyList();
    }
    
    List<Node> results = new ArrayList<>();
    Long taskStartId = ProcessUtils.getTaskStartIdFromPID(processStart.pid().toString());
    List<ICase> cases = getAllCasesFromTaskStartIdWithTimeInterval(taskStartId, timeIntervalFilter, selectedCustomFilters);
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFromIProcessWebStartable(processStart);
    extractedArrowFromProcessElements(processElements, results);
    updateFrequencyForNodes(results, processElements, cases);
    results.stream().forEach(node -> updateNodeByAnalysisType(node, analysisType));
    return results;
  }

  /**
   * For this version, we cover 2 simple cases: + Process without alternative. +
   * Process with 1 alternative.
   **/
  private static List<Node> updateFrequencyForNodes(List<Node> results, List<ProcessElement> processElements,
      List<ICase> cases) {
    List<Alternative> alternatives = ProcessUtils.extractAlterNativeElementsWithMultiOutGoing(processElements);
    if (CollectionUtils.isEmpty(alternatives)) {
      results.stream().forEach(node -> updateNodeWiwthDefinedFrequency(cases.size(), node));
    } else {
      alternatives.stream().forEach(alternative -> handleFrequencyForAlternativePath(alternative, results, cases));
    }
    return results;
  }

  private static void handleFrequencyForAlternativePath(Alternative alternative, List<Node> results,
      List<ICase> cases) {
    List<AlternativePath> paths = new ArrayList<>();
    List<SequenceFlow> mainFlowFromAlternative = alternative.getOutgoing();
    mainFlowFromAlternative.forEach(path -> {
      AlternativePath currentPath = new AlternativePath();
      currentPath.setOriginFlow(path);
      currentPath.setNodeIdsInPath(new ArrayList<>());
      followPath(currentPath, path);
      paths.add(currentPath);
    });
    updateFrequencyForCaseWithSimpleAlternative(paths, results, cases);
    results.stream().forEach(node -> node.setRelativeValue((float) node.getFrequency()/cases.size()));
  }

  private static void updateFrequencyForCaseWithSimpleAlternative(List<AlternativePath> paths, List<Node> results,
      List<ICase> cases) {
    cases.stream().forEach(currentCase -> {
      List<String> taskIdDoneInCase = currentCase.tasks().all().stream()
          .map(iTask -> ProcessUtils.getTaskElementIdFromRequestPath(iTask.getRequestPath())).toList();

      Optional<AlternativePath> runningPathOpt = paths.stream()
          .filter(path -> taskIdDoneInCase.contains(path.getTaskSwitchEventIdOnPath())).findFirst()
          .or(() -> paths.stream().filter(path -> StringUtils.isBlank(path.getTaskSwitchEventIdOnPath())).findFirst());

      List<String> nonRunningElements = new ArrayList<>();
      paths.stream().filter(path -> !runningPathOpt.equals(Optional.of(path))).forEach(path -> {
        nonRunningElements.addAll(path.getNodeIdsInPath());
        nonRunningElements.add(ProcessUtils.getElementPid(path.getOriginFlow()));
      });

      results.stream().filter(node -> !nonRunningElements.contains(node.getId()))
          .forEach(node -> node.setFrequency(node.getFrequency() + 1));
    });
  }

  private static void followPath(AlternativePath path, SequenceFlow currentFlow) {
    ProcessElement destinationElement = (ProcessElement) currentFlow.getTarget();
    path.getNodeIdsInPath().add(ProcessUtils.getElementPid(currentFlow));
    if (ProcessUtils.isTaskSwitchEvent(destinationElement)) {
      path.setTaskSwitchEventIdOnPath(ProcessUtils.getElementPid(destinationElement));
    }
    if (destinationElement.getIncoming().size() > 1) {
      return;
    }
    path.getNodeIdsInPath().add(ProcessUtils.getElementPid(destinationElement));
    destinationElement.getOutgoing().forEach(outGoingPath -> followPath(path, outGoingPath));
  }

  private static List<ICase> getAllCasesFromTaskStartIdWithTimeInterval(Long taskStartId,
      TimeIntervalFilter timeIntervalFilter, Map<ICustomFieldMeta, Object> selectedCustomFilters) {
    CaseQuery query = CaseQuery.create().where().state().isEqual(CaseState.DONE).and().taskStartId()
        .isEqual(taskStartId).and().startTimestamp().isGreaterOrEqualThan(timeIntervalFilter.getFrom()).and()
        .startTimestamp().isLowerOrEqualThan(timeIntervalFilter.getTo());

    if (ObjectUtils.isNotEmpty(selectedCustomFilters)) {
      CaseQuery subQuery = null;
      for (Map.Entry<ICustomFieldMeta, Object> entry : selectedCustomFilters.entrySet()) {
        ICustomFieldMeta customFieldMeta = entry.getKey();
        Object customFieldValue = entry.getValue();

        addCustomFieldCondition(query, customFieldMeta, customFieldValue);
      }
      query.where().andOverall(subQuery);
    }
    return Ivy.wf().getCaseQueryExecutor().getResults(query);
  }

  private static void addCustomFieldCondition(CaseQuery subQuery, ICustomFieldMeta customFieldMeta,
      Object customFieldValue) {
    String customFieldName = customFieldMeta.name();
    CustomFieldType customFieldType = customFieldMeta.type();
    String stringValue = (String) customFieldValue;
    switch (customFieldType) {
      case STRING:
        subQuery.where().and().customField().stringField(customFieldName).isEqual(stringValue).or()
            .tasks(TaskQuery.create().where().customField().stringField(customFieldName).isEqual(stringValue));
        break;
      case TEXT:
        subQuery.where().and().customField().textField(customFieldName).isEqual(stringValue).or()
            .tasks(TaskQuery.create().where().customField().textField(customFieldName).isEqual(stringValue));
        break;
      case NUMBER:
        Number numberValue =
            (customFieldValue instanceof Number) ? (Number) customFieldValue : Double.parseDouble(stringValue);
        subQuery.where().and().customField().numberField(customFieldName).isEqual(numberValue).or()
            .tasks(TaskQuery.create().where().customField().numberField(customFieldName).isEqual(numberValue));
        break;
      case TIMESTAMP:
        Date dateValue = (customFieldValue instanceof String) ? DateUtils.parseDateFromString(customFieldName)
            : (Date) customFieldValue;
        subQuery.where().and().customField().timestampField(customFieldName).isEqual(dateValue).or()
            .tasks(TaskQuery.create().where().customField().timestampField(customFieldName).isEqual(dateValue));
        break;
      default:
        break;
    }
  }

  private static void updateNodeWiwthDefinedFrequency(int value, Node node) {
    Long releativeValue = (long) (value == 0 ? 0: 1);
    node.setRelativeValue(releativeValue);
    node.setLabelValue(Objects.requireNonNullElse(value, 0));
    node.setFrequency(value);
  }

}
