package com.axonivy.solutions.process.analyser.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
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

  /**
   * Get more additional insight from process viewer (task count, number of
   * instances with interval time range,...) when user click "show statistic
   * data".
   * 
   * @param pid rawPid of selected process
   */

  public static List<Node> convertProcessElementInfoToNode(ProcessElement element) {
    return element.getOutgoing().stream().map(flow -> convertSequenceFlowToNode(flow)).collect(Collectors.toList());
  }

  public static Node convertSequenceFlowToNode(SequenceFlow flow) {
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
    return elementNode;
  }

  public static void updateNodeByAnalysisType(Node node, KpiType analysisType) {
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
   * New approach to show process analyser data without modifying original process.
   * All of material which is use to analyzing will be based on task data from
   * AxonIvy system db.
   **/
  public static List<Node> filterInitialStatisticByIntervalTime(IProcessWebStartable processStart,
      KpiType analysisType, List<ICase> cases) {
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
   * frequency for element belong to its.
   **/
  public static List<Node> updateFrequencyForNodes(List<Node> results, List<ProcessElement> processElements,
      List<ICase> cases) {
    List<Alternative> alternatives = ProcessUtils.extractAlterNativeElementsWithMultiOutGoing(processElements);
    if (CollectionUtils.isEmpty(alternatives)) {
      results.stream().forEach(node -> updateNodeWiwthDefinedFrequency(cases.size(), node));
    } else {
      alternatives.stream().forEach(alternative -> handleFrequencyForAlternativePath(alternative, results, cases));
    }
    return results;
  }

  public static void handleFrequencyForAlternativePath(Alternative alternative, List<Node> results,
      List<ICase> cases) {
    if (ObjectUtils.anyNull(alternative, results, cases)) {
      return;
    }
    List<SequenceFlow> mainFlowFromAlternative = alternative.getOutgoing();
    List<AlternativePath> paths = mainFlowFromAlternative.stream()
        .map(ProcessesMonitorUtils::convertSequenceFlowToAlternativePath).toList();
    updateFrequencyForCaseWithAlternative(paths, results, cases);
    results.stream().forEach(node -> node.setRelativeValue((float) node.getFrequency()/cases.size()));
  }

  public static AlternativePath convertSequenceFlowToAlternativePath(SequenceFlow flow) {
    AlternativePath path = new AlternativePath();
    path.setOriginFlowId(ProcessUtils.getElementPid(flow));
    path.setNodeIdsInPath(new ArrayList<>());
    followPath(path, flow);
    return path;
  }

  public static void updateFrequencyForCaseWithAlternative(List<AlternativePath> paths, List<Node> results,
      List<ICase> cases) {
    cases.stream().forEach(currentCase -> {
      List<String> taskIdDoneInCase = currentCase.tasks().all().stream()
          .map(iTask -> ProcessUtils.getTaskElementIdFromRequestPath(iTask.getRequestPath())).toList();
      List<String> nonRunningElements = paths.stream()
          .filter(path -> taskIdDoneInCase.contains(path.getTaskSwitchEventIdOnPath()))
          .flatMap(path -> path.getNodeIdsInPath().stream()).toList();
      results.stream().filter(node -> !nonRunningElements.contains(node.getId()))
          .forEach(node -> node.setFrequency(node.getFrequency() + 1));
    });
  }

  /**
   * Collect all of elements in current flow. When the flow reach other
   * alternative, element that is also an end element of other flow or the last
   * element in that flow
   **/
  public static void followPath(AlternativePath path, SequenceFlow currentFlow) {
    ProcessElement destinationElement = (ProcessElement) currentFlow.getTarget();
    path.getNodeIdsInPath().add(ProcessUtils.getElementPid(currentFlow));
    if (ProcessUtils.isTaskSwitchEvent(destinationElement)
        || StringUtils.isNotBlank(path.getTaskSwitchEventIdOnPath())) {
      path.setTaskSwitchEventIdOnPath(ProcessUtils.getElementPid(destinationElement));
    }
    if (ProcessUtils.isEndElementOfAlternativePath(destinationElement)) {
      return;
    }
    path.getNodeIdsInPath().add(ProcessUtils.getElementPid(destinationElement));
    destinationElement.getOutgoing().forEach(outGoingPath -> followPath(path, outGoingPath));
  }

  /**
   * Construct the base query and, if applicable, a sub-query for custom fields. 
   * For custom fields of type NUMBER or TIMESTAMP, ensure exactly two values are provided. 
   * For STRING type fields, iterate over each value to build individual sub-queries.
   * If no customFieldValues are found, subQuery will be ignored
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
        addCustomFieldSubQueryForTimestamp(customFieldQuery, customFieldFilter, customFieldFilter.getTimestampCustomFieldValues());
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
   * Cast values to right type and Create sub-query for each custom field type from case or task
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
          customFieldQuery.where().or().customField().numberField(customFieldName).isGreaterOrEqualThan(startNumber)
              .and().customField().numberField(customFieldName).isLowerOrEqualThan(endNumber);
        } else {
          customFieldQuery.where().or()
              .tasks(TaskQuery.create().where().customField().numberField(customFieldName)
                  .isGreaterOrEqualThan(startNumber).and().customField().numberField(customFieldName)
                  .isLowerOrEqualThan(endNumber));
        }
        break;
      default:
        break;
    }
  }

  public static void updateNodeWiwthDefinedFrequency(int value, Node node) {
    Long releativeValue = (long) (value == 0 ? 0: 1);
    node.setRelativeValue(releativeValue);
    node.setLabelValue(Objects.requireNonNullElse(value, 0));
    node.setFrequency(value);
  }
}
