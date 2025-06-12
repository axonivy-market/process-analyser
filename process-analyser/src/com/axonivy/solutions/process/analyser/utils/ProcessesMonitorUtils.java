package com.axonivy.solutions.process.analyser.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
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

  public static List<Node> convertToNodes(List<ProcessElement> processElements, List<SequenceFlow> sequenceFlows) {
    return Stream
        .concat(processElements.stream().flatMap(pe -> ProcessesMonitorUtils.convertProcessElementToNode(pe).stream()),
            sequenceFlows.stream().map(ProcessesMonitorUtils::convertSequenceFlowToNode))
        .collect(Collectors.toList());
  }

  /** 
   * Convert process element to Node base on its class type 
  **/
  public static List<Node> convertProcessElementToNode(ProcessElement element) {
    Node node = createNode(element.getPid().toString(), element.getName(), NodeType.ELEMENT);
    node.setOutGoingPathIds(element.getOutgoing().stream().map(ProcessUtils::getElementPid).toList());

    return switch (element) {
    case TaskSwitchGateway taskSwitchGateway -> {
      node.setInCommingPathIds(taskSwitchGateway.getIncoming().stream().map(ProcessUtils::getElementPid).toList());
      node.setTaskSwitchGateway(true);
      String elementId = taskSwitchGateway.getPid().toString();
      List<Node> taskNodes = taskSwitchGateway.getAllTaskConfigs().stream()
          .map(task -> createNode(
              elementId + ProcessAnalyticsConstants.SLASH + task.getTaskIdentifier().getTaskIvpLinkName(),
              task.getName().getRawMacro(), NodeType.ELEMENT))
          .collect(Collectors.toList());
      taskNodes.add(0, node);
      yield taskNodes;
    }
    case RequestStart requestStart -> {
      node.setRequestPath(requestStart.getRequestPath().getLinkPath());
      yield List.of(node);
    }
    default -> {
      node.setInCommingPathIds(element.getIncoming().stream().map(ProcessUtils::getElementPid).toList());
      yield List.of(node);
    }};
  }

  public static Node convertSequenceFlowToNode(SequenceFlow flow) {
    Node node = createNode(ProcessUtils.getElementPid(flow), flow.getName(), NodeType.ARROW);
    node.setTargetNodeId(flow.getTarget().getPid().toString());
    node.setSourceNodeId(flow.getSource().getPid().toString());
    return node;
  }

  private static Node createNode(String id, String label, NodeType type) {
    Node node = new Node();
    node.setId(id);
    node.setLabel(label);
    node.setType(type);
    return node;
  }

  public static void updateNodeByAnalysisType(Node node, KpiType analysisType) {
    if (KpiType.FREQUENCY == analysisType) {
      node.setLabelValue(String.valueOf(node.getFrequency()));
    } else {
      String medianDurationValue = DateUtils.convertDuration(node.getMedianDuration());
      node.setLabelValue(medianDurationValue);
      node.setFormattedMedianDuration(medianDurationValue);
    }
    if (Double.isNaN(node.getRelativeValue())) {
      node.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    }
  }

  public static List<Node> filterInitialStatisticByIntervalTime(IProcessWebStartable processStart, KpiType analysisType,
      List<ICase> cases) {
    if (Objects.isNull(processStart)) {
      return Collections.emptyList();
    }
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFrom(processStart);
    if (isDuration(analysisType)) {
      processElements = ProcessUtils.getTaskStart(processElements);
    }
    List<SequenceFlow> sequenceFlows = ProcessUtils.getSequenceFlowsFrom(processElements);
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

  public static void updateDurationForNodes(List<Node> nodes, List<ICase> cases, KpiType durationKpiType) {
    if (CollectionUtils.isEmpty(nodes) || CollectionUtils.isEmpty(cases)) {
      return;
    }

    // Extract node IDs and request paths
    Set<String> taskSwitchPids = extractNodeAttributes(nodes, Node::getId);
    Set<String> requestPaths = extractNodeAttributes(nodes, Node::getRequestPath);
    Set<String> taskSwitchGatewayPids = nodes.stream()
        .filter(node -> node != null && node.getId() != null && node.isTaskSwitchGateway()).map(Node::getId)
        .collect(Collectors.toSet());

    Function<ITask, Long> durationExtractor = getDurationExtractor(durationKpiType);

    // Group task durations based on their extracted task ID
    Map<String, List<Long>> nodeDurations = cases.stream().flatMap(currentCase -> currentCase.tasks().all().stream())
        .filter(task -> isValidTask(task, taskSwitchPids, requestPaths))
        .collect(Collectors.groupingBy(task -> determineTaskKey(task, taskSwitchGatewayPids, requestPaths),
            Collectors.mapping(durationExtractor, Collectors.toList())));

    // Remove nodes that are TaskSwitchGateways or have no valid durations
    nodes.removeIf(node -> shouldRemoveNode(node, nodeDurations));
    updateRelativeValueForDuration(nodes);
  }

  private static void updateRelativeValueForDuration(List<Node> nodes) {
    List<Node> arrows = nodes.stream().filter(node -> node.getType() == NodeType.ARROW)
        .sorted(Comparator.comparingDouble(Node::getMedianDuration)).toList();

    arrows.forEach(arrNode -> arrNode
        .setRelativeValue(arrNode.getMedianDuration() / arrows.get(arrows.size() - 1).getMedianDuration()));
  }

  private static Set<String> extractNodeAttributes(List<Node> nodes, Function<Node, String> attributeExtractor) {
    return nodes.stream().map(attributeExtractor).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private static boolean isValidTask(ITask task, Set<String> taskSwitchPids, Set<String> requestPaths) {
    String taskId = ProcessUtils.getTaskElementIdFromRequestPath(task.getRequestPath());
    return taskSwitchPids.contains(taskId)
        || (!task.getRequestPath().isBlank() && requestPaths.contains(task.getRequestPath()));
  }

  private static String determineTaskKey(ITask task, Set<String> taskSwitchGatewayPids, Set<String> requestPaths) {
    String taskId = ProcessUtils.getTaskElementIdFromRequestPath(task.getRequestPath());
    if (taskSwitchGatewayPids.contains(taskId)) {
      return ProcessUtils.getTaskElementIdFromRequestPath(task.getRequestPath(), true);
    }
    if (!task.getRequestPath().isBlank() && requestPaths.contains(task.getRequestPath())) {
      return task.getRequestPath();
    }
    return taskId;
  }

  private static boolean shouldRemoveNode(Node node, Map<String, List<Long>> nodeDurations) {
    if (node.isTaskSwitchGateway()) {
      return true;
    }
    String key = StringUtils.isBlank(node.getRequestPath()) ? node.getId() : node.getRequestPath();
    String lookupKey = node.getType() == NodeType.ARROW ? node.getSourceNodeId() : key;
    List<Long> durations = nodeDurations.get(lookupKey);
    if (CollectionUtils.isEmpty(durations)) {
      return true;
    }
    node.setMedianDuration(calculateMedian(durations));
    return false;
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
    return (long) Math.ceil((task.getEndTimestamp().getTime() - task.getStartTimestamp().getTime()) / 1000.0);
  }

  private static float calculateMedian(List<Long> durations) {
    if (durations.isEmpty())
      return 0f;
    List<Long> sorted = durations.stream().sorted().toList();
    int middle = sorted.size() / 2;

    return sorted.size() % 2 == 0 ? (sorted.get(middle - 1) + sorted.get(middle)) / 2.0f : sorted.get(middle);
  }

  /**
   * . If
   * not, we need to check which path from alternative is running to update
   * frequency for elements belong to its.
   **/
  public static void updateFrequencyForNodes(List<Node> results, List<ProcessElement> processElements,
      List<ICase> cases) {
    List<ProcessElement> branchSwitchingElement = ProcessUtils.getAlterNativesWithMultiOutgoings(processElements);

    // If current process have no alternative -> frequency = totals cases size.
    if (CollectionUtils.isEmpty(branchSwitchingElement)) {
      results.stream().forEach(node -> updateNodeWithDefinedFrequency(cases.size(), node));
      return;
    }

    // check which path from alternative is running to update frequency for elements belong to its.
    List<ProcessElement> alternativeEnds = ProcessUtils.getElementsWithMultiIncomings(processElements);
    branchSwitchingElement.addAll(alternativeEnds);
    List<AlternativePath> paths = convertToAternativePaths(branchSwitchingElement);
    handleFrequencyForCasesWithAlternativePaths(paths, results, cases);
    List<ProcessElement> taskSwitchElements = processElements.stream()
        .filter(element -> ProcessUtils.isTaskSwitchGatewayInstance(element)).collect(Collectors.toList());
    List<ProcessElement> complexElements = new ArrayList<>();
    complexElements.addAll(taskSwitchElements);
    complexElements.addAll(branchSwitchingElement);
    List<ProcessElement> subWithOneIncommingElements = processElements.stream()
        .filter(element -> ProcessUtils.isEmbeddedElementInstance(element)
            && !ProcessUtils.isElementWithMultipleIncomingFlow(element))
        .collect(Collectors.toList());
    complexElements.addAll(subWithOneIncommingElements);
    updateFrequencyForComplexElements(complexElements, results);
    updateRelativeValueForNodes(results);

  }

  public static void updateFrequencyForComplexElements(List<ProcessElement> complexElements, List<Node> nodes) {
    if (ObjectUtils.anyNull(complexElements, nodes)) {
      return;
    }

    complexElements.stream().forEach(processElement -> {
      Node node = findNodeById(processElement.getPid().toString(), nodes);
      int frequency = 0;
      for (String inCommingPathId : node.getInCommingPathIds()) {
        frequency += getFrequencyById(inCommingPathId, nodes);
      }
      node.setFrequency(frequency);

      if (node.getOutGoingPathIds().size() == 1) {
        Node outGoingPath = findNodeById(node.getOutGoingPathIds().getFirst(), nodes);
        outGoingPath.setFrequency(frequency);
        return;
      }

      for (String outGoingPathId : node.getOutGoingPathIds()) {
        Node outGoingPath = findNodeById(outGoingPathId, nodes);
        Node targetNodeOfOutGoingPath = findNodeById(outGoingPath.getTargetNodeId(), nodes);
        outGoingPath.setFrequency(targetNodeOfOutGoingPath.getFrequency());
      }
    });
  }

  public static int getFrequencyById(String id, List<Node> nodes) {
    return nodes.stream().filter(node -> node.getId().equals(id)).map(Node::getFrequency).findFirst()
        .orElseGet(() -> 0);
  }

  public static Node findNodeById(String id, List<Node> nodes) {
    return nodes.stream().filter(node -> node.getId().equals(id)).findFirst().orElseGet(() -> null);
  }

  public static void updateRelativeValueForNodes(List<Node> nodes) {
    if (CollectionUtils.isEmpty(nodes)) {
      return;
    }

    int maxFrequency = 1;
    for (Node node : nodes) {
      if (node.getFrequency() > maxFrequency) {
        maxFrequency = node.getFrequency();
      }
    }

    for (Node node : nodes) {
      node.setRelativeValue((float) node.getFrequency() / maxFrequency);
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
      List<String> taskIdsDoneInCase = getTaskIdDoneInCase(currentCase);
      List<String> nonRunningElementIdsInCase = getNonRunningElementIdsInCase(taskIdsDoneInCase, paths);

      Map<String, Integer> hashMap = countFrequencyOfTask(taskIdsDoneInCase);
      for (Node node : results) {
        if (hashMap.containsKey(node.getId()) && CollectionUtils.isNotEmpty(node.getOutGoingPathIds())) {
          for (String pathId : node.getOutGoingPathIds()) {
            hashMap.put(pathId, hashMap.get(node.getId()));
          }
        }
      }

      results.stream().filter(node -> !nonRunningElementIdsInCase.contains(node.getId()))
          .forEach(node -> node.setFrequency(node.getFrequency() + hashMap.getOrDefault(node.getId(), 1)));
    });
  }

  public static List<String> getTaskIdDoneInCase(ICase currentCase) {
    return currentCase.tasks().all().stream()
        .map(iTask -> ProcessUtils.getTaskElementIdFromRequestPath(iTask.getRequestPath())).toList();
  }

  public static Map<String, Integer> countFrequencyOfTask(List<String> taskIdsDoneInCase) {
    // Create HashMap to store the count
    Map<String, Integer> idCountMap = new HashMap<>();

    // Count occurrences
    for (String id : taskIdsDoneInCase) {
      idCountMap.put(id, idCountMap.getOrDefault(id, 0) + 1);
    }

    return idCountMap;
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

  public static List<String> getNonRunningElementIdsInCase(List<String> taskIdsDoneInCase,
      List<AlternativePath> paths) {
    List<String> results = new ArrayList<String>();
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
    ProcessElement destinationElement = ProcessElement.class.cast(currentFlow.getTarget());
    String flowPid = ProcessUtils.getElementPid(currentFlow);
    String destinationElementPid = ProcessUtils.getElementPid(destinationElement);

    path.getNodeIdsInPath().add(flowPid);
    updateTaskSwitchEventIdOnPath(path, destinationElement);
    if (ProcessUtils.isAlternativePathEndElement(destinationElement)) {
      return;
    }

    path.getNodeIdsInPath().add(destinationElementPid);
    if (ProcessUtils.isEmbeddedElementInstance(destinationElement)) {
      destinationElement = ProcessUtils.getEmbeddedStartConnectToFlow(destinationElement, flowPid);
    } else if (ProcessUtils.isSubProcessCallInstance(destinationElement)) {
      ProcessElement callStart = ProcessUtils.getStartElementFromSubProcessCall(destinationElement);
      addAndTraverseOutgoing(path, callStart);
    }

    addAndTraverseOutgoing(path, destinationElement);
  }

  private static void addAndTraverseOutgoing(AlternativePath path, ProcessElement element) {
    path.getNodeIdsInPath().add(ProcessUtils.getElementPid(element));
    element.getOutgoing().forEach(outgoing -> followPath(path, outgoing));
  }

  private static void updateTaskSwitchEventIdOnPath(AlternativePath path, ProcessElement destinationElement) {
    if (ProcessUtils.isTaskSwitchInstance(destinationElement)
        && StringUtils.isBlank(path.getTaskSwitchEventIdOnPath())) {
      path.setTaskSwitchEventIdOnPath(ProcessUtils.getElementPid(destinationElement));
    }
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

  public static void updateNodeWithDefinedFrequency(int value, Node node) {
    Long releativeValue = (long) (value == 0 ? 0 : 1);
    node.setRelativeValue(releativeValue);
    node.setLabelValue(Objects.requireNonNullElse(value, 0).toString());
    node.setFrequency(value);
  }
}
