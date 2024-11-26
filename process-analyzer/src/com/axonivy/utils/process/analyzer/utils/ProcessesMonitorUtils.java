package com.axonivy.utils.process.analyzer.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.process.analyzer.bo.AlternativePath;
import com.axonivy.utils.process.analyzer.bo.Node;
import com.axonivy.utils.process.analyzer.bo.TimeIntervalFilter;
import com.axonivy.utils.process.analyzer.constants.ProcessAnalyticsConstants;
import com.axonivy.utils.process.analyzer.enums.KpiType;
import com.axonivy.utils.process.analyzer.enums.NodeType;
import com.axonivy.utils.process.analyzer.internal.ProcessUtils;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.workflow.CaseState;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.query.CaseQuery;
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
  public static void extractNodesFromProcessElements(List<ProcessElement> processElements, List<Node> results) {
    processElements.forEach(element -> {
      results.add(convertProcessElementToNode(element));
      results.addAll(convertProcessElementInfoToNode(element));
      if (ProcessUtils.isEmbeddedElementInstance(element)) {
        extractNodesFromProcessElements(ProcessUtils.getNestedProcessElementsFromSub(element), results);
      }
    });
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
   * New approach to show process analyzer data without modifying original process.
   * All of material which is use to analyzing will be based on task data from
   * AxonIvy system db.
   **/
  public static List<Node> filterInitialStatisticByIntervalTime(IProcessWebStartable processStart,
      TimeIntervalFilter timeIntervalFilter, KpiType analysisType) {
    if (Objects.isNull(processStart)) {
      return Collections.emptyList();
    }
    
    List<Node> results = new ArrayList<>();
    Long taskStartId = ProcessUtils.getTaskStartIdFromPID(processStart.pid().toString());
    List<ICase> cases = getAllCasesFromTaskStartIdWithTimeInterval(taskStartId, timeIntervalFilter);
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFromIProcessWebStartable(processStart);
    extractNodesFromProcessElements(processElements, results);
    updateFrequencyForNodes(results, processElements, cases);
    results.stream().forEach(node -> updateNodeByAnalysisType(node, analysisType));
    return results;
  }

  /**
   * For this version, we cover 2 simple cases: + Process without alternative. +
   * Process with 1 alternative.
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

  public static void updateFrequencyForCaseWithSimpleAlternative(List<AlternativePath> paths, List<Node> results,
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

  public static void followPath(AlternativePath path, SequenceFlow currentFlow) {
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

  public static List<ICase> getAllCasesFromTaskStartIdWithTimeInterval(Long taskStartId,
      TimeIntervalFilter timeIntervalFilter) {
    return CaseQuery.create().where().state().isEqual(CaseState.DONE).and().taskStartId().isEqual(taskStartId).and()
        .startTimestamp().isGreaterOrEqualThan(timeIntervalFilter.getFrom()).and().startTimestamp()
        .isLowerOrEqualThan(timeIntervalFilter.getTo()).executor().results();
  }

  public static void updateNodeWiwthDefinedFrequency(int value, Node node) {
    Long releativeValue = (long) (value == 0 ? 0: 1);
    node.setRelativeValue(releativeValue);
    node.setLabelValue(Objects.requireNonNullElse(value, 0));
    node.setFrequency(value);
  }
}