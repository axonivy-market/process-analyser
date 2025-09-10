package com.axonivy.solutions.process.analyser.resolver;

import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.SLASH;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.Path;
import com.axonivy.solutions.process.analyser.bo.TaskPath;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.core.util.PIDUtils;
import com.axonivy.solutions.process.analyser.enums.PathStatus;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.activity.SubProcessCall;
import ch.ivyteam.ivy.process.model.element.event.end.CallSubEnd;
import ch.ivyteam.ivy.process.model.element.event.end.EmbeddedEnd;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;

@SuppressWarnings("restriction")
public class NodeFrequencyResolver {
  private static final Pattern SEQUENCE_FLOW_CONDITION_PATTERN = Pattern.compile("ivp==\\\"([^\\\"]+)\\\"");
  private List<Node> nodes;
  private List<ProcessElement> processElements;

  public NodeFrequencyResolver(List<Node> nodes, List<ProcessElement> processElements) {
    Objects.requireNonNull(processElements, "ProcessElements must not be null");
    Objects.requireNonNull(nodes, "Nodes must not be empty");
    this.processElements = processElements;
    this.nodes = nodes;
  }

  /**
   * The updateFrequencyForNodes does: Loops the tasks of case then detects which
   * is the target created by the selected task Then, found the shorten path from
   * start point to target point Turn up frequency for found node id
   * 
   * @param cases is list of cases created from selected process
   */
  public void updateFrequencyByCases(List<ICase> cases) {
    if (CollectionUtils.isEmpty(cases)) {
      return;
    }

    cases.forEach(ivyCase -> {
      for (var task : ivyCase.tasks().all()) {
        List<String> nodeIdsInPath = findShortestWayFromTaskStartToEnd(task, processElements);
        updateFrequencyForNodeById(nodes, nodeIdsInPath);
      }
    });
  }

  /**
   * Increase the frequency of found node to 1
   * 
   * @param nodes         is list of nodes from selected ivyProcess
   * @param nodeIdsInPath is list of nodeId that the ivyTask visited
   */
  private static void updateFrequencyForNodeById(List<Node> nodes, List<String> nodeIdsInPath) {
    for (var nodeId : nodeIdsInPath) {
      nodes.stream().filter(node -> node.getId().contentEquals(nodeId))
          .forEach(node -> node.setFrequency(node.getFrequency() + 1));
    }
  }

  /**
   * A task always has start point and end point - Start point can be a start or
   * task element - End point can be a task or end element The function will find
   * the shortest way to go from 'Start' to 'End' The found way will contain many
   * path, each path contains list of node id
   * 
   * @param task:            selecting ivy task
   * @param processElements: all process elements in the process the task belongs
   *                         to
   * @return foundNodeIdsInPath: list of nodes related to the found way
   */
  private static List<String> findShortestWayFromTaskStartToEnd(ITask task, List<ProcessElement> processElements) {
    var taskStartPid = task.getStartSwitchEvent().getTaskElement().getProcessElementId();
    var taskEndPid = task.getEndSwitchEvent().getTaskElement().getProcessElementId();

    List<String> foundNodeIdsInPath = new ArrayList<>();
    var firstPath = new Path(new ArrayList<>());
    TaskPath taskPath = new TaskPath(task.uuid(), new ArrayList<>());
    taskPath.setPaths(new ArrayList<>());
    taskPath.getPaths().add(firstPath);

    List<ProcessElement> subProcessCalls = filterSubProcessCallElements(processElements);
    ProcessElement taskProcessElement = findProcessElementStartedTask(taskStartPid, processElements);
    if (taskProcessElement == null) {
      // Process was change, then task do not belong to process anymore
      return new ArrayList<>();
    }

    List<SequenceFlow> outGoingFlows = taskProcessElement.getOutgoing();
    if (ProcessUtils.isTaskSwitchGatewayInstance(taskProcessElement)) {
      outGoingFlows = detectOutGoingCreatedTaskFromTaskGetaway(task.getRequestPath(), taskProcessElement);
    }
    // Loops all out going follow to collect the node id
    outGoingFlows.forEach(out -> {
      firstPath.setStartPathId(ProcessUtils.getElementPid(out));
      followNodes(taskEndPid, firstPath, out, subProcessCalls, taskPath);
      if (taskPath.isFinished()) {
        Path foundTargetPath = taskPath.getPaths().stream().filter(Path::isFound).findAny().get();
        var startPathId = foundTargetPath.getStartPathId();
        foundNodeIdsInPath.addAll(foundTargetPath.getNodesInPath());
        List<Path> excludePathsNotFound = taskPath.getPaths().stream()
            .filter(path -> path.getStatus() != PathStatus.NOT_FOUND).toList();
        collectNodeIdsFromStartPathIdInFoundPaths(excludePathsNotFound, foundNodeIdsInPath, startPathId);
        return;
      }
    });
    // Add the 'start' point and remove 'end' point to avoid duplication counter
    foundNodeIdsInPath.add(0, taskStartPid);
    foundNodeIdsInPath.remove(taskEndPid);
    return foundNodeIdsInPath;
  }

  private static void followNodes(String targetPid, Path path, SequenceFlow currentFlow,
      List<ProcessElement> subProcessCalls, TaskPath taskPath) {
    if (path.isFound()) {
      return;
    }

    String flowPid = ProcessUtils.getElementPid(currentFlow);
    path.getNodesInPath().add(flowPid);
    if (flowPid.contentEquals(targetPid)) {
      return;
    }

    ProcessElement destinationElement = ProcessElement.class.cast(currentFlow.getTarget());
    if (isEndOfPathByGivenProcessElement(targetPid, path, destinationElement)) {
      return;
    }

    ProcessElement nextElement = resolveNextElementForNode(path.getNodesInPath(), destinationElement, flowPid);
    // Handle for case: destinationElement is a CallSubEnd and there is no
    // alternative in the SubProcessCall
    if (nextElement == null && destinationElement instanceof CallSubEnd) {
      nextElement = getNestedSubElement(destinationElement, subProcessCalls);
    }

    if (nextElement != destinationElement) {
      if (isEndOfPathByGivenProcessElement(targetPid, path, nextElement)) {
        return;
      }
      // if a sub directly connect to another sub then check
      if (ProcessUtils.isEmbeddedEndInstance(destinationElement)
          && ProcessUtils.isEmbeddedElementInstance(nextElement)) {
        nextElement = getNextElementForSubToSub(destinationElement, nextElement);
      }
    }

    if (nextElement instanceof CallSubEnd) {
      nextElement = getNestedSubElement(nextElement, subProcessCalls);
    }

    List<SequenceFlow> nextOutgoingFlows = getNextOutgoingFlows(nextElement);
    var isAlternative = ProcessUtils.isAlternativeInstance(nextElement);
    // If next element is task switch, then finish current path and create new path
    if (isAlternative || nextOutgoingFlows.size() > 1) {
      String nextElementPid = ProcessUtils.getElementPid(nextElement);
      finishCurrentPathAndOpenNewPathForNextFlows(targetPid, path, subProcessCalls, taskPath, nextElementPid,
          nextOutgoingFlows);
    } else {
      followNodes(targetPid, path, nextOutgoingFlows.get(0), subProcessCalls, taskPath);
    }
  }

  private static void finishCurrentPathAndOpenNewPathForNextFlows(String targetPid, Path path,
      List<ProcessElement> subProcessCalls, TaskPath taskPath, String nextElementPid,
      List<SequenceFlow> nextOutgoingFlows) {
    path.setStatus(PathStatus.FINISHED);
    path.setEndPathId(nextElementPid);

    for (var outgoing : nextOutgoingFlows) {
      if (taskPath.isFinished()) {
        return;
      }
      var startNewPath = new Path(nextElementPid, new ArrayList<>());
      taskPath.getPaths().add(startNewPath);
      followNodes(targetPid, startNewPath, outgoing, subProcessCalls, taskPath);
    }
  }

  private static ProcessElement findProcessElementStartedTask(String taskStartProcessElementId,
      List<ProcessElement> processElements) {
    return CollectionUtils.emptyIfNull(processElements).stream()
        .filter(element -> PIDUtils.getId(element.getPid()).contentEquals(taskStartProcessElementId)).findAny()
        .orElse(null);
  }

  private static List<ProcessElement> filterSubProcessCallElements(List<ProcessElement> processElements) {
    return CollectionUtils.emptyIfNull(processElements).stream().filter(SubProcessCall.class::isInstance).toList();
  }

  private static void collectNodeIdsFromStartPathIdInFoundPaths(List<Path> paths, List<String> nodesInPath,
      String startPathId) {
    for (var path : paths) {
      if (path.getEndPathId().contentEquals(startPathId)) {
        nodesInPath.addAll(0, path.getNodesInPath());
        startPathId = path.getStartPathId();
        collectNodeIdsFromStartPathIdInFoundPaths(paths, nodesInPath, startPathId);
      }
    }
  }

  private static ProcessElement getNextElementForSubToSub(ProcessElement destinationElement,
      ProcessElement nextElement) {
    for (var in : nextElement.getIncoming()) {
      if (in.getEmbeddedSource().isPresent()
          && PIDUtils.equalsPID(in.getEmbeddedSource().get().getPid(), destinationElement.getPid())
          && in.getEmbeddedTarget().isPresent()) {
        nextElement = ProcessElement.class.cast(in.getEmbeddedTarget().get());
        break;
      }
    }
    return nextElement;
  }

  private static boolean isEndOfPathByGivenProcessElement(String targetProcessElementId, Path path,
      ProcessElement processElement) {
    var processElementId = ProcessUtils.getElementPid(processElement);
    path.getNodesInPath().add(processElementId);
    if (processElementId.contentEquals(targetProcessElementId)) {
      path.setStatus(PathStatus.FOUND);
      path.setEndPathId(processElementId);
      return true;
    } else if (ProcessUtils.isTaskSwitchInstance(processElement) || ProcessUtils.isTaskEndInstance(processElement)) {
      path.setStatus(PathStatus.NOT_FOUND);
      path.setEndPathId(processElementId);
      return true;
    }
    return false;
  }

  private static ProcessElement getNestedSubElement(ProcessElement element, List<ProcessElement> subProcessCalls) {
    return subProcessCalls.stream().filter(subProcessCall -> isSubProcessCallContainElement(element, subProcessCall))
        .findAny().orElse(null);
  }

  private static List<SequenceFlow> detectOutGoingCreatedTaskFromTaskGetaway(String taskRequestPath,
      ProcessElement processElement) {
    List<SequenceFlow> outGoingFlows = processElement.getOutgoing();
    var processElementIdPrefix = PIDUtils.getId(processElement.getPid()).concat(SLASH);
    for (var out : processElement.getOutgoing()) {
      String endCondition = out.getCondition();
      Matcher matcher = SEQUENCE_FLOW_CONDITION_PATTERN.matcher(out.getCondition());
      if (matcher.find()) {
        String value = matcher.group(1);
        endCondition = processElementIdPrefix.concat(value);
      }
      if (!taskRequestPath.endsWith(endCondition)) {
        outGoingFlows.remove(out);
      }
    }
    return outGoingFlows;
  }

  private static boolean isSubProcessCallContainElement(ProcessElement element, ProcessElement subProcessCall) {
    return ProcessUtils.getNestedProcessElementsFromSub(subProcessCall).stream().map(ProcessUtils::getElementPid)
        .toList().contains(ProcessUtils.getElementPid(element));
  }

  private static List<SequenceFlow> getNextOutgoingFlows(ProcessElement nextElement) {
    return (nextElement instanceof EmbeddedEnd embeddedEnd) ? List.of(embeddedEnd.getConnectedOuterSequenceFlow())
        : nextElement.getOutgoing();
  }

  private static ProcessElement resolveNextElementForNode(List<String> nodesInPath, ProcessElement element,
      String currentFlowPid) {
    return switch (element) {
    case EmbeddedProcessElement embedded -> ProcessUtils.getEmbeddedStartConnectToFlow(embedded, currentFlowPid);
    case CallSubEnd callSubEnd -> element;
    case SubProcessCall subProcess -> ProcessUtils.getStartElementFromSubProcessCall(subProcess);
    case EmbeddedEnd embeddedEnd -> {
      SequenceFlow outerFlow = embeddedEnd.getConnectedOuterSequenceFlow();
      nodesInPath.add(ProcessUtils.getElementPid(outerFlow));
      yield ProcessElement.class.cast(outerFlow.getTarget());
    }
    default -> element;
    };
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  public List<ProcessElement> getProcessElements() {
    return processElements;
  }

  public void setProcessElements(List<ProcessElement> processElements) {
    this.processElements = processElements;
  }
}
