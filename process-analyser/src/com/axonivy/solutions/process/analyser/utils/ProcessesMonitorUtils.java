package com.axonivy.solutions.process.analyser.utils;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.bo.AlternativePath;
import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.workflow.CaseState;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.custom.field.CustomFieldType;
import ch.ivyteam.ivy.workflow.query.CaseQuery;
import ch.ivyteam.ivy.workflow.query.TaskQuery;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;

@SuppressWarnings("restriction")
public class ProcessesMonitorUtils {
  private ProcessesMonitorUtils() {
  }

  public static Node convertSequenceFlowToNode(SequenceFlow flow) {
    Node arrowNode = new Node();
    arrowNode.setId(ProcessUtils.getElementPid(flow));
    arrowNode.setLabel(flow.getName());
    arrowNode.setFrequency(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    arrowNode.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
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
  public static List<Node> convertToNodes(List<ProcessElement> processElements, List<SequenceFlow> sequenceFlows) {
    return Stream.concat(processElements.stream().map(ProcessesMonitorUtils::convertProcessElementToNode),
        sequenceFlows.stream().map(ProcessesMonitorUtils::convertSequenceFlowToNode)).collect(Collectors.toList());
  }

  public static Node convertProcessElementToNode(ProcessElement element) {
    Node elementNode = new Node();
    elementNode.setId(element.getPid().toString());
    elementNode.setLabel(element.getName());
    elementNode.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    elementNode.setFrequency(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    elementNode.setMedianDuration(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_DURATION_NUMBER);
    elementNode.setType(NodeType.ELEMENT);
    return elementNode;
  }

  public static void updateNodeByAnalysisType(Node node, KpiType analysisType) {
    if (KpiType.FREQUENCY == analysisType) {
      node.setLabelValue(String.valueOf(node.getFrequency()));
    } else {
      node.setLabelValue(formatDuration(convertDuration(node.getMedianDuration(), analysisType)));
    }
    if (Double.isNaN(node.getRelativeValue())) {
      node.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    }
  }

  private static String formatDuration(float value) {
    DecimalFormat df = new DecimalFormat("#.##");
    return df.format(value);
  }

  /**
   * New approach to show process analyser data without modifying original
   * process. All of material which is use to analyzing will be based on task data
   * from AxonIvy system db.
   **/
  public static List<Node> filterInitialStatisticByIntervalTime(IProcessWebStartable processStart, KpiType analysisType,
      List<ICase> cases) {
    if (Objects.isNull(processStart)) {
      return Collections.emptyList();
    }
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFrom(processStart);
    List<SequenceFlow> sequenceFlows = getSequenceFlowsIfNeeded(processElements, analysisType);
    List<Node> nodes = convertToNodes(processElements, sequenceFlows);
    if (isFrequency(analysisType)) {
      updateFrequencyForNodes(nodes, processElements, cases);
    } else if (isDuration(analysisType)) {
      updateDurationForNodes(nodes, cases, analysisType);
    }
    nodes.forEach(node -> updateNodeByAnalysisType(node, analysisType));
    return nodes;
  }

  private static boolean isFrequency(KpiType kpiType) {
    return KpiType.FREQUENCY.equals(kpiType);
  }

  public static boolean isDuration(KpiType kpiType) {
    return kpiType != null && kpiType.isDescendantOf(KpiType.DURATION);
  }

  private static List<SequenceFlow> getSequenceFlowsIfNeeded(List<ProcessElement> processElements, KpiType kpiType) {
    return (isFrequency(kpiType)) ? ProcessUtils.getSequenceFlowsFrom(processElements) : List.of();
  }

  public static void updateDurationForNodes(List<Node> nodes, List<ICase> cases, KpiType durationKpiType) {
    if (CollectionUtils.isEmpty(nodes) || CollectionUtils.isEmpty(cases)) {
      return;
    }

    List<String> taskSwitchPids = nodes.stream().filter(node -> node != null && node.getId() != null).map(Node::getId)
        .collect(Collectors.toList());

    Function<ITask, Long> durationExtractor = getDurationExtractor(durationKpiType);

    Map<String, List<Long>> nodeDurations = cases.stream()
        // Extract all tasks from all cases into a single stream
        .flatMap(currentCase -> currentCase.tasks().all().stream())

        // Filter tasks based on their ID (extracted from request path)
        .filter(task -> taskSwitchPids.contains(ProcessUtils.getTaskElementIdFromRequestPath(task.getRequestPath())))

        // Group tasks by their extracted task ID
        .collect(Collectors.groupingBy(task -> ProcessUtils.getTaskElementIdFromRequestPath(task.getRequestPath()),

            // Extract the duration of each task and collect into a list
            Collectors.mapping(durationExtractor, Collectors.toList())));

    nodes.removeIf(node -> {
      List<Long> durations = nodeDurations.get(node.getId());
      if (durations == null || durations.isEmpty()) {
        return true;
      }
      node.setMedianDuration(calculateMedian(durations));
      return false;
    });
  }

  private static Function<ITask, Long> getDurationExtractor(KpiType durationKpiType) {
    if (durationKpiType.isDescendantOf(KpiType.DURATION_IDLE)) {
      return task -> getOverallDuration(task) - task.getWorkingTime().toNumber();
    } else if (durationKpiType.isDescendantOf(KpiType.DURATION_OVERALL)) {
      return task -> getOverallDuration(task);
    } else if (durationKpiType.isDescendantOf(KpiType.DURATION_WORKING)) {
      return task -> task.getWorkingTime().toNumber();
    }
    return task -> 0L;
  }
  
  private static long getOverallDuration(ITask task) {
    return (task.getEndTimestamp().getTime() - task.getStartTimestamp().getTime()) / 1000;
  }

  private static float calculateMedian(List<Long> durations) {
    if (durations.isEmpty())
      return 0f;
    List<Long> sorted = durations.stream().sorted().toList();
    int middle = sorted.size() / 2;

    return sorted.size() % 2 == 0 ? (sorted.get(middle - 1) + sorted.get(middle)) / 2.0f : sorted.get(middle);
  }

  private static float convertDuration(float durationSeconds, KpiType kpiType) {
    return switch (kpiType) {
    case DURATION_IDLE_DAY, DURATION_WORKING_DAY, DURATION_OVERALL_DAY ->
      (float) (durationSeconds / (60 * 60 * 24));
    case DURATION_IDLE_HOUR, DURATION_WORKING_HOUR, DURATION_OVERALL_HOUR ->
      (float) (durationSeconds / (60 * 60));
    case DURATION_IDLE_MINUTE, DURATION_WORKING_MINUTE, DURATION_OVERALL_MINUTE ->
      (float) (durationSeconds / (60));
    default -> durationSeconds;
    };
  }

  /**
   * If current process have no alternative -> frequency = totals cases size. If
   * not, we need to check which path from alternative is running to update
   * frequency for elements belong to its.
   **/
  public static void updateFrequencyForNodes(List<Node> results, List<ProcessElement> processElements,
      List<ICase> cases) {
    List<ProcessElement> branchSwitchingElement = ProcessUtils.getAlterNativesWithMultiOutgoings(processElements);
    if (CollectionUtils.isEmpty(branchSwitchingElement)) {
      results.stream().forEach(node -> updateNodeWiwthDefinedFrequency(cases.size(), node));
    } else {
      List<ProcessElement> alternativeEnds = ProcessUtils.getElementsWithMultiIncomings(processElements);
      branchSwitchingElement.addAll(alternativeEnds);
      List<AlternativePath> paths = convertToAternativePaths(branchSwitchingElement);
      handleFrequencyForCasesWithAlternativePaths(paths, results, cases);
    }
  }

  /**
   * If current process have no alternative -> frequency = totals cases size. If
   * not, we need to check which path from alternative is running to update
   * frequency for elements belong to its.
   **/
  public static void handleFrequencyForCasesWithAlternativePaths(List<AlternativePath> paths, List<Node> results,
      List<ICase> cases) {
    if (ObjectUtils.anyNull(paths, results, cases)) {
      return;
    }
    cases.stream().forEach(currentCase -> {
      List<String> nonRunningElementIdsInCase = getNonRunningElementIdsInCase(currentCase, paths);
      results.stream().filter(node -> !nonRunningElementIdsInCase.contains(node.getId()))
          .forEach(node -> node.setFrequency(node.getFrequency() + 1));
    });
    results.stream().forEach(node -> node.setRelativeValue((float) node.getFrequency() / cases.size()));
  }

  public static List<AlternativePath> convertToAternativePaths(List<ProcessElement> elements) {
    return elements.stream().flatMap(element -> convertToAlternativePaths(element).stream()).toList();
  }

  public static List<AlternativePath> convertToAlternativePaths(ProcessElement element) {
    boolean isAlternativeEnd = !ProcessUtils.isAlternativeInstance(element);
    List<String> precedingFlowId = isAlternativeEnd
        ? element.getIncoming().stream().map(ProcessUtils::getElementPid).toList()
        : new ArrayList<String>();
    return element.getOutgoing().stream().map(flow -> convertSequenceFlowToAlternativePath(flow, precedingFlowId))
        .toList();
  }

  public static AlternativePath convertSequenceFlowToAlternativePath(SequenceFlow flow, List<String> precedingFlowIds) {
    AlternativePath path = new AlternativePath();
    path.setPathFromAlternativeEnd(CollectionUtils.isNotEmpty(precedingFlowIds));
    path.setPrecedingFlowIds(precedingFlowIds);
    path.setNodeIdsInPath(new ArrayList<>());
    followPath(path, flow);
    return path;
  }

  public static List<String> getNonRunningElementIdsInCase(ICase currentCase, List<AlternativePath> paths) {
    List<String> results = new ArrayList<String>();
    List<String> taskIdsDoneInCase = currentCase.tasks().all().stream()
        .map(iTask -> ProcessUtils.getTaskElementIdFromRequestPath(iTask.getRequestPath())).toList();
    List<String> nonRunningElementIdsFromAlternative = paths.stream()
        .filter(
            path -> !path.isPathFromAlternativeEnd() && !taskIdsDoneInCase.contains(path.getTaskSwitchEventIdOnPath()))
        .flatMap(path -> path.getNodeIdsInPath().stream()).toList();
    List<String> nonRunningElementIdsFromEndElements = getNonRunningElementIdsFromAlternativeEnds(paths,
        nonRunningElementIdsFromAlternative);
    results.addAll(nonRunningElementIdsFromAlternative);
    results.addAll(nonRunningElementIdsFromEndElements);
    return results;
  }

  public static List<String> getNonRunningElementIdsFromAlternativeEnds(List<AlternativePath> paths,
      List<String> nonRunningElementIdsFromAlternative) {
    return paths.stream()
        .filter(path -> path.isPathFromAlternativeEnd()
            && CollectionUtils.containsAll(nonRunningElementIdsFromAlternative, path.getPrecedingFlowIds()))
        .flatMap(path -> path.getNodeIdsInPath().stream()).toList();
  }

  /**
   * Collect all of elements in current flow. When the flow reach other
   * alternative, element that is also an end element of other flow or the last
   * element in that flow
   **/
  public static void followPath(AlternativePath path, SequenceFlow currentFlow) {
    ProcessElement destinationElement = (ProcessElement) currentFlow.getTarget();
    path.getNodeIdsInPath().add(ProcessUtils.getElementPid(currentFlow));
    if (ProcessUtils.isTaskSwitchInstance(destinationElement)
        && StringUtils.isBlank(path.getTaskSwitchEventIdOnPath())) {
      path.setTaskSwitchEventIdOnPath(ProcessUtils.getElementPid(destinationElement));
    }
    if (ProcessUtils.isAlternativePathEndElement(destinationElement)) {
      return;
    }
    path.getNodeIdsInPath().add(ProcessUtils.getElementPid(destinationElement));
    destinationElement.getOutgoing().forEach(outgoingPath -> followPath(path, outgoingPath));
  }

  /**
   * Construct the base query and, if applicable, a sub-query for custom fields.
   * For custom fields of type NUMBER or TIMESTAMP, ensure exactly two values are
   * provided. For STRING type fields, iterate over each value to build individual
   * sub-queries. If no customFieldValues are found, subQuery will be ignored
   **/
  public static List<ICase> getAllCasesFromTaskStartIdWithTimeInterval(Long taskStartId,
      TimeIntervalFilter timeIntervalFilter, List<CustomFieldFilter> customFilters) {
    CaseQuery query = CaseQuery.create().where().state().isEqual(CaseState.DONE).and().taskStartId()
        .isEqual(taskStartId).and().startTimestamp().isGreaterOrEqualThan(timeIntervalFilter.getFrom()).and()
        .startTimestamp().isLowerOrEqualThan(timeIntervalFilter.getTo());

    List<CustomFieldFilter> validCustomFilters = getValidCustomFilters(customFilters);
    if (ObjectUtils.isNotEmpty(validCustomFilters)) {
      CaseQuery allCustomFieldsQuery = CaseQuery.create();

      for (CustomFieldFilter customFieldFilter : validCustomFilters) {
        CaseQuery customFieldQuery = CaseQuery.create();
        handleQueryForEachFieldType(customFieldFilter, customFieldQuery);

        allCustomFieldsQuery.where().and(customFieldQuery);
      }
      query.where().andOverall(allCustomFieldsQuery);
    }
    return Ivy.wf().getCaseQueryExecutor().getResults(query);
  }

  private static List<CustomFieldFilter> getValidCustomFilters(List<CustomFieldFilter> customFilters) {
    return customFilters.stream().filter(filter -> ObjectUtils.isNotEmpty(filter.getCustomFieldValues())
        || ObjectUtils.isNotEmpty(filter.getTimestampCustomFieldValues())).collect(Collectors.toList());
  }

  private static void handleQueryForEachFieldType(CustomFieldFilter customFieldFilter, CaseQuery customFieldQuery) {
    CustomFieldType customFieldType = customFieldFilter.getCustomFieldMeta().type();

    switch (customFieldType) {
    case TIMESTAMP:
      addCustomFieldSubQueryForTimestamp(customFieldQuery, customFieldFilter,
          customFieldFilter.getTimestampCustomFieldValues());
      break;
    case NUMBER:
      addCustomFieldSubQuery(customFieldQuery, customFieldFilter, customFieldFilter.getCustomFieldValues());
      break;
    case STRING:
    case TEXT:
      for (Object customFieldValue : customFieldFilter.getCustomFieldValues()) {
        addCustomFieldSubQuery(customFieldQuery, customFieldFilter, customFieldValue);
      }
      break;
    default:
      break;
    }
  }

  private static void addCustomFieldSubQueryForTimestamp(CaseQuery customFieldQuery,
      CustomFieldFilter customFieldFilter, List<LocalDate> timestampCustomFieldValues) {
    boolean isCustomFieldFromCase = customFieldFilter.isCustomFieldFromCase();
    String customFieldName = customFieldFilter.getCustomFieldMeta().name();

    Date startDate = DateUtils.getDateFromLocalDate(timestampCustomFieldValues.get(0), null);
    Date endDate = DateUtils.getDateFromLocalDate(timestampCustomFieldValues.get(1), LocalTime.MAX);

    if (isCustomFieldFromCase) {
      customFieldQuery.where().or().customField().timestampField(customFieldName).isGreaterOrEqualThan(startDate).and()
          .customField().timestampField(customFieldName).isLowerOrEqualThan(endDate);
    } else {
      customFieldQuery.where().or().tasks(
          TaskQuery.create().where().customField().timestampField(customFieldName).isGreaterOrEqualThan(startDate).and()
              .customField().timestampField(customFieldName).isLowerOrEqualThan(endDate));
    }
  }

  /**
   * Cast values to right type and Create sub-query for each custom field type
   * from case or task
   **/
  @SuppressWarnings("unchecked")
  private static void addCustomFieldSubQuery(CaseQuery customFieldQuery, CustomFieldFilter customFieldFilter,
      Object customFieldValue) {
    boolean isCustomFieldFromCase = customFieldFilter.isCustomFieldFromCase();
    String customFieldName = customFieldFilter.getCustomFieldMeta().name();
    CustomFieldType customFieldType = customFieldFilter.getCustomFieldMeta().type();

    switch (customFieldType) {
    case STRING:
      String stringValue = (String) customFieldValue;
      if (isCustomFieldFromCase) {
        customFieldQuery.where().or().customField().stringField(customFieldName).isEqual(stringValue);
      } else {
        customFieldQuery.where().or()
            .tasks(TaskQuery.create().where().customField().stringField(customFieldName).isEqual(stringValue));
      }
      break;
    case TEXT:
      String textValue = (String) customFieldValue;
      if (isCustomFieldFromCase) {
        customFieldQuery.where().or().customField().textField(customFieldName).isEqual(textValue);
      } else {
        customFieldQuery.where().or()
            .tasks(TaskQuery.create().where().customField().textField(customFieldName).isEqual(textValue));
      }
      break;
    case NUMBER:
      Double startNumber;
      Double endNumber;
      try {
        List<String> numberRange = (List<String>) customFieldValue;
        startNumber = Double.parseDouble(numberRange.get(0));
        endNumber = Double.parseDouble(numberRange.get(1));
      } catch (Exception e) {
        List<Double> numberRange = (List<Double>) customFieldValue;
        startNumber = numberRange.get(0);
        endNumber = numberRange.get(1);
      }

      if (isCustomFieldFromCase) {
        customFieldQuery.where().or().customField().numberField(customFieldName).isGreaterOrEqualThan(startNumber).and()
            .customField().numberField(customFieldName).isLowerOrEqualThan(endNumber);
      } else {
        customFieldQuery.where().or().tasks(
            TaskQuery.create().where().customField().numberField(customFieldName).isGreaterOrEqualThan(startNumber)
                .and().customField().numberField(customFieldName).isLowerOrEqualThan(endNumber));
      }
      break;
    default:
      break;
    }
  }

  public static void updateNodeWiwthDefinedFrequency(int value, Node node) {
    Long releativeValue = (long) (value == 0 ? 0 : 1);
    node.setRelativeValue(releativeValue);
    node.setLabelValue(Objects.requireNonNullElse(value, 0).toString());
    node.setFrequency(value);
  }
}
