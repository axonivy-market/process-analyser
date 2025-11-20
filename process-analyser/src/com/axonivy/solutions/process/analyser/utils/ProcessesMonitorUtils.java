package com.axonivy.solutions.process.analyser.utils;

import static com.axonivy.solutions.process.analyser.constants.AnalyserConstants.PROCESS_ANALYTIC_PERSISTED_CONFIG;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.ProcessAnalyser;
import com.axonivy.solutions.process.analyser.bo.ProcessViewerConfig;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.bo.StartElement;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.core.util.ProcessElementUtils;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.resolver.NodeFrequencyResolver;
import com.axonivy.solutions.process.analyser.resolver.NodeResolver;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.workflow.CaseState;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.custom.field.CustomFieldType;
import ch.ivyteam.ivy.workflow.query.CaseQuery;
import ch.ivyteam.ivy.workflow.query.TaskQuery;

@SuppressWarnings("restriction")
public class ProcessesMonitorUtils {
  private static final String KEY_SEPARATOR = ":::";

  private ProcessesMonitorUtils() { }

  public static List<Node> filterInitialStatisticByIntervalTime(ProcessAnalyser processAnalyser, KpiType analysisType,
      List<ICase> cases) {
    if (Objects.isNull(processAnalyser)) {
      return Collections.emptyList();
    }
    return (processAnalyser.getStartElement() == null) ? filterForMergedStarts(processAnalyser, analysisType, cases)
        : filterForSingleStart(processAnalyser, analysisType, cases);
  }

  private static List<Node> filterForMergedStarts(ProcessAnalyser processAnalyser, KpiType analysisType,
      List<ICase> cases) {
    var pmv = processAnalyser.getProcess().getPmv();
    String processId = processAnalyser.getProcess().getId();
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFrom(processId, pmv);

    if (isDuration(analysisType)) {
      processElements = ProcessUtils.getTaskStart(processElements);
    }

    return buildNodesAndApplyKpi(processElements, analysisType, cases);
  }

  private static List<Node> filterForSingleStart(ProcessAnalyser processAnalyser, KpiType analysisType,
      List<ICase> cases) {
    String startElementPID = processAnalyser.getStartElement().getPid();
    String processId = processAnalyser.getProcess().getId();
    var pmv = processAnalyser.getProcess().getPmv();
    List<ProcessElement> processElements = collectProcessElementForProcess(pmv, processId, startElementPID);

    if (isDuration(analysisType)) {
      processElements = ProcessUtils.getTaskStart(processElements);
    }

    return buildNodesAndApplyKpi(processElements, analysisType, cases);
  }

  private static List<Node> buildNodesAndApplyKpi(List<ProcessElement> processElements, KpiType analysisType,
      List<ICase> cases) {
    List<SequenceFlow> sequenceFlows = ProcessUtils.getSequenceFlowsFrom(processElements);
    List<Node> nodes = NodeResolver.convertToNodes(processElements, sequenceFlows);

    if (isFrequency(analysisType)) {
      var nodeFrequencyResolver = new NodeFrequencyResolver(nodes, processElements);
      nodeFrequencyResolver.updateFrequencyByCases(cases);
      nodes = nodeFrequencyResolver.getNodes();
    } else if (isDuration(analysisType)) {
      updateDurationForNodes(nodes, cases, analysisType);
    }

    return NodeResolver.updateNodeByAnalysisType(nodes, analysisType);
  }

  private static List<ProcessElement> collectProcessElementForProcess(IProcessModelVersion pmv, String processId,
      String startElementPID) {
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFrom(processId, pmv);
    ProcessElementUtils.removeAnotherStartElementsBySelectedStartPID(processElements, startElementPID);
    return processElements;
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

    if (arrows.isEmpty()) {
      return;
    }

    float maxMedian = arrows.getLast().getMedianDuration();
    if (maxMedian == 0) {
      return; // avoid division by zero
    }

    nodes.forEach(arrNode -> arrNode.setRelativeValue(arrNode.getMedianDuration() / maxMedian));
  }

  private static Set<String> extractNodeAttributes(List<Node> nodes, Function<Node, String> attributeExtractor) {
    return nodes.stream().map(attributeExtractor).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private static boolean isValidTask(ITask task, Set<String> taskSwitchPids, Set<String> requestPaths) {
    String taskId = ProcessUtils.getTaskElementId(task);
    return taskSwitchPids.contains(taskId)
        || (!task.getRequestPath().isBlank() && requestPaths.contains(task.getRequestPath()));
  }

  private static String determineTaskKey(ITask task, Set<String> taskSwitchGatewayPids, Set<String> requestPaths) {
    String taskId = ProcessUtils.getTaskElementId(task);
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
   * Construct the base query and, if applicable, a sub-query for custom fields.
   * For custom fields of type NUMBER or TIMESTAMP, ensure exactly two values are
   * provided. For STRING type fields, iterate over each value to build individual
   * sub-queries. If no customFieldValues are found, subQuery will be ignored
   * @param isIncludingRunningCases 
   **/
  public static List<ICase> getAllCasesFromTaskStartIdWithTimeInterval(Long taskStartId,
      TimeIntervalFilter timeIntervalFilter, List<CustomFieldFilter> customFilters, boolean isIncludingRunningCases,
      IProcessModelVersion processModelVersion) {
    var caseQuery = CaseQuery.create();
    caseQuery.where().and(buildCaseStateQuery(isIncludingRunningCases)).and(buildTaskStartIdQuery(taskStartId))
        .and(buildStartTimestampQuery(timeIntervalFilter));

    List<CustomFieldFilter> validCustomFilters = getValidCustomFilters(customFilters);
    if (ObjectUtils.isNotEmpty(validCustomFilters)) {
      CaseQuery allCustomFieldsQuery = CaseQuery.create();

      for (CustomFieldFilter customFieldFilter : validCustomFilters) {
        CaseQuery customFieldQuery = CaseQuery.create();
        handleQueryForEachFieldType(customFieldFilter, customFieldQuery);

        allCustomFieldsQuery.where().and(customFieldQuery);
      }
      caseQuery.where().andOverall(allCustomFieldsQuery);
    }
//    Sudo.run(() -> {
//      CaseQuery.businessCases().executor().results().forEach(caze -> {
//        Ivy.log().warn("Tasktask id ne : " + caze.getProcessStart().getTaskStart().getId());
//        Ivy.log().error("No Filter total size {0} {1} {2} {3} {4}", caze.uuid(),
//            caze.getProcessStart().getFullRequestPath(), caze.getProcessStart().getTaskStart().getId(),
//            caze.getProcessModelVersion().getVersionName());
//      });
//    });
//
    var output1 = Ivy.wf().getCaseQueryExecutor().getResults(caseQuery);
//    Ivy.log().error("total size {0}", output1.size());

    return output1.stream().filter(ca -> {
//      Ivy.log().warn("Version name ne: " + ca.getProcessModelVersion().getVersionName());
      return ca.getProcessModelVersion().equals(processModelVersion);
    }).toList();
  }

  private static CaseQuery buildStartTimestampQuery(TimeIntervalFilter timeIntervalFilter) {
    return CaseQuery.create().where().startTimestamp().isGreaterOrEqualThan(timeIntervalFilter.getFrom())
        .and()
        .startTimestamp().isLowerOrEqualThan(timeIntervalFilter.getTo());
  }

  private static CaseQuery buildTaskStartIdQuery(Long taskStartId) {
    return CaseQuery.create().where().taskStartId().isEqual(taskStartId);
  }

  private static CaseQuery buildCaseStateQuery(boolean isIncludingRunningCases) {
    var query = CaseQuery.create();
    if (!isIncludingRunningCases) {
      query.where().state().isEqual(CaseState.DONE);
    }
    return query;
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
  
  public static ProcessViewerConfig getUserConfig() {
    String persistedConfigString = Ivy.session().getSessionUser().getProperty(PROCESS_ANALYTIC_PERSISTED_CONFIG);
    return StringUtils.isBlank(persistedConfigString) ? new ProcessViewerConfig()
        : JacksonUtils.fromJson(persistedConfigString, ProcessViewerConfig.class);
  }
  
  public static void updateUserProperty(ProcessViewerConfig persistedConfig) {
    Ivy.session().getSessionUser().setProperty(PROCESS_ANALYTIC_PERSISTED_CONFIG,
        JacksonUtils.convertObjectToJSONString(persistedConfig));
  }
  
  public static ProcessAnalyser mappingProcessAnalyzerByProcesses(List<Process> processElements,
      boolean isMergeProcessStarts, String processKeyId) {
    String[] data = processKeyId.split(KEY_SEPARATOR);
    var foundProcess = processElements.stream()
        .filter(element -> element.getId().equals(data[1]))
        .findAny()
        .orElse(null);
    
    StartElement foundStartElement = null;
    if (!isMergeProcessStarts && foundProcess != null) {
      foundStartElement = foundProcess.getStartElements()
          .stream()
          .filter(start -> start.getPid().equals(data[2]))
          .findFirst()
          .orElse(null);
    }
    
    var processAnalyser = new ProcessAnalyser();
    processAnalyser.setProcess(foundProcess);
    processAnalyser.setStartElement(foundStartElement);
    processAnalyser.setProcessKeyId(processKeyId);
    return processAnalyser;
  }
}
