package com.axonivy.solutions.process.analyser.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.bo.AlternativePath;
import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;
import com.axonivy.solutions.process.analyser.enums.IvyVariable;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.workflow.CaseState;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.custom.field.CustomFieldType;
import ch.ivyteam.ivy.workflow.query.CaseQuery;
import ch.ivyteam.ivy.workflow.query.TaskQuery;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;

@SuppressWarnings("restriction")
public class ProcessesMonitorUtils {
  private ProcessesMonitorUtils() {
  }

  public static List<Node> convertProcessElementInfoToNode(ProcessElement element) {
    return element.getOutgoing().stream().map(flow -> {
      return convertSequenceFlowToNode(flow);
    }).collect(Collectors.toList());
  }

  public static Node convertSequenceFlowToNode(SequenceFlow flow) {
    String elementId = ProcessUtils.getElementPid(flow);
    Node arrowNode = new Node();
    arrowNode.setId(elementId);
    arrowNode.setLabel(flow.getName());
    arrowNode.setFrequency(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    arrowNode.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    arrowNode.setMedianDuration(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    arrowNode.setType(NodeType.ARROW);
    arrowNode.setTargetNodeId(flow.getTarget().getPid().toString());
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
  public static List<Node> extractNodesFromProcessElements(List<ProcessElement> processElements) {
    List<Node> results = new ArrayList<>();
    processElements.forEach(element -> {
      results.add(convertProcessElementToNode(element));
      results.addAll(convertProcessElementInfoToNode(element));
      if (ProcessUtils.isEmbeddedElementInstance(element)) {
        results.addAll(extractNodesFromProcessElements(ProcessUtils.getNestedProcessElementsFromSub(element)));
      }
    });
    return results;
  }

  public static Node convertProcessElementToNode(ProcessElement element) {
    Node elementNode = new Node();
    elementNode.setId(element.getPid().toString());
    elementNode.setLabel(element.getName());
    elementNode.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    elementNode.setFrequency(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    elementNode.setMedianDuration(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    elementNode.setType(NodeType.ELEMENT);
    elementNode.setOutGoingPathIds(element.getOutgoing().stream().map(ProcessUtils::getElementPid).toList());
    elementNode.setInCommingPathIds(element.getIncoming().stream().map(ProcessUtils::getElementPid).toList());
    return elementNode;
  }

  public static void updateNodeByAnalysisType(Node node, KpiType analysisType) {
    if (KpiType.FREQUENCY == analysisType) {
      node.setLabelValue(node.getFrequency());
    } else {
      node.setLabelValue((int) Math.round(node.getMedianDuration()));
    }
    if (Double.isNaN(node.getRelativeValue())) {
      node.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    }
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
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFromIProcessWebStartable(processStart);
    
    List<Node> results = extractNodesFromProcessElements(processElements);
    updateFrequencyForNodes(results, processElements, cases);
    results.stream().forEach(node -> updateNodeByAnalysisType(node, analysisType));
    return results;
  }

  /**
   * If current process have no alternative -> frequency = totals cases size. If
   * not, we need to check which path from alternative is running to update
   * frequency for elements belong to its.
   **/
  public static List<Node> updateFrequencyForNodes(List<Node> results, List<ProcessElement> processElements,
      List<ICase> cases) {
    List<ProcessElement> branchSwitchingElement = ProcessUtils.getAlterNativesWithMultiOutgoings(processElements);
    if (CollectionUtils.isEmpty(branchSwitchingElement)) {
      results.stream().forEach(node -> updateNodeWiwthDefinedFrequency(cases.size(), node));
    } else {
      List<ProcessElement> alternativeEnds = ProcessUtils.getElementsWithMultiIncomings(processElements);
      branchSwitchingElement.addAll(alternativeEnds);
      List<AlternativePath> paths = convertToAternativePaths(branchSwitchingElement);
      handleFrequencyForCasesWithAlternativePaths(paths, results, cases);

      List<ProcessElement> taskSwitchElements = processElements.stream()
          .filter(element -> ProcessUtils.isTaskSwitchGatewayInstance(element)).collect(Collectors.toList());
      List<ProcessElement> complexElements = new ArrayList<>();
      complexElements.addAll(taskSwitchElements);
      complexElements.addAll(branchSwitchingElement);
      updateFrequencyForComplexElements(complexElements, results, cases);
    }

    return results;
  }

  private static void updateFrequencyForComplexElements(List<ProcessElement> complexElements, List<Node> nodes,
      List<ICase> cases) {
    if (ObjectUtils.anyNull(complexElements, nodes) || CollectionUtils.isEmpty(cases)) {
      return;
    }

    complexElements.stream().forEach(processElement -> {
      Node node = findNodeById(processElement.getPid().toString(), nodes);
      int frequency = 0;
      for (String inCommingPathId : node.getInCommingPathIds()) {
        frequency += getFrequencyById(inCommingPathId, nodes);
      }
      node.setFrequency(frequency);
      node.setRelativeValue((float) (frequency / cases.size())
          / Integer.valueOf(Ivy.var().get(IvyVariable.MAX_REWORK_TIME_IN_A_CASE.getVariableName())));
      for (String outGoingPathId : node.getOutGoingPathIds()) {
        Node outGoingPath = findNodeById(outGoingPathId, nodes);
        Node targetNodeOfOutGoingPath = findNodeById(outGoingPath.getTargetNodeId(), nodes);
        outGoingPath.setFrequency(targetNodeOfOutGoingPath.getFrequency());
        outGoingPath.setRelativeValue((float) (targetNodeOfOutGoingPath.getFrequency() / cases.size())
            / Integer.valueOf(Ivy.var().get(IvyVariable.MAX_REWORK_TIME_IN_A_CASE.getVariableName())));
      }
    });
  }

  private static int getFrequencyById(String id, List<Node> nodes) {
    return nodes.stream().filter(node -> node.getId().equals(id)).map(Node::getFrequency).findFirst().orElseGet(() -> 0);
  }

  private static Node findNodeById(String id, List<Node> nodes) {
    return nodes.stream().filter(node -> node.getId().equals(id)).findFirst().orElseGet(() -> null);
  }

  /**
   * If current process have no alternative -> frequency = totals cases size. If
   * not, we need to check which path from alternative is running to update
   * frequency for elements belong to its.
   **/
  public static void handleFrequencyForCasesWithAlternativePaths(List<AlternativePath> paths, List<Node> results,
      List<ICase> cases) {
    if (ObjectUtils.anyNull(paths, results) || CollectionUtils.isEmpty(cases)) {
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
    results.stream().forEach(node -> node.setRelativeValue((float) (node.getFrequency() / cases.size())
        / Integer.valueOf(Ivy.var().get(IvyVariable.MAX_REWORK_TIME_IN_A_CASE.getVariableName()))));
  }

  private static List<String> getTaskIdDoneInCase(ICase currentCase) {
    return currentCase.tasks().all().stream()
        .map(iTask -> ProcessUtils.getTaskElementIdFromRequestPath(iTask.getRequestPath())).toList();
  }

  private static Map<String, Integer> countFrequencyOfTask(List<String> taskIdsDoneInCase) {
    // Create HashMap to store the count
    Map<String, Integer> idCountMap = new HashMap<>();

    // Count occurrences
    for (String id : taskIdsDoneInCase) {
      idCountMap.put(id, idCountMap.getOrDefault(id, 0) + 1);
      // count task in the sub element and use this frequency for the sub element
      // Not support for multi-tasks in the sub yet
      if (id.split(ProcessAnalyticsConstants.HYPHEN_SIGN).length == 3) {
        String subId = id.split(ProcessAnalyticsConstants.HYPHEN_SIGN)[0] + ProcessAnalyticsConstants.HYPHEN_SIGN
            + id.split(ProcessAnalyticsConstants.HYPHEN_SIGN)[1];
        idCountMap.put(subId, idCountMap.getOrDefault(subId, 0) + 1);
      }
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

  public static List<String> getNonRunningElementIdsInCase(List<String> taskIdsDoneInCase, List<AlternativePath> paths) {
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
    node.setLabelValue(Objects.requireNonNullElse(value, 0));
    node.setFrequency(value);
  }
}
