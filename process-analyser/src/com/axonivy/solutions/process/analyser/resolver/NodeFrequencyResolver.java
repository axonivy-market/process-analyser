package com.axonivy.solutions.process.analyser.resolver;

import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.SLASH;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
import ch.ivyteam.ivy.workflow.ITaskElement;
import ch.ivyteam.ivy.workflow.ITaskSwitchEvent;

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
   * Updates the frequency values of nodes based on the execution paths found in
   * the given cases.
   * For each provided case, this method iterates through all
   * associated tasks, identifies the shortest path in the process from the task's
   * start to its end, and increments the frequency for each node encountered
   * along that path.
   * After processing all cases and tasks, it recalculates the
   * relative values for all nodes.
   *
   * @param cases the list of cases created from the selected process; if
   *              {@code null} or empty, the method returns immediately
   */
  public void updateFrequencyByCases(List<ICase> cases) {
    if (CollectionUtils.isEmpty(cases)) {
      return;
    }

    cases.forEach(ivyCase -> {
      List<ITask> finishedTasks = ivyCase.tasks().all().stream().filter(ITask::isPersistent).toList();
      for (var task : finishedTasks) {
        List<String> nodeIdsInPath = findShortestWayFromTaskStartToEnd(task, processElements);
        updateFrequencyForNodeById(nodes, nodeIdsInPath);
      }
    });
    NodeResolver.updateRelativeValueForNodes(nodes);
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
    var taskStartPid = extractTaskSwitchEventElementPid(task.getStartSwitchEvent());
    var taskEndPid = extractTaskSwitchEventElementPid(task.getEndSwitchEvent());

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

  private static String extractTaskSwitchEventElementPid(ITaskSwitchEvent taskSwitchEvent) {
    return Optional.ofNullable(taskSwitchEvent).map(ITaskSwitchEvent::getTaskElement)
        .map(ITaskElement::getProcessElementId).orElse(null);
  }

  /**
   * Follows the process flow from the current node, updating the path with
   * visited node IDs, and recursively traversing according to the process
   * definition.
   *
   * @param targetPid       The target process element ID to search for.
   * @param path            The current path object being built and updated.
   * @param currentFlow     The outgoing sequence flow from the current node.
   * @param subProcessCalls List of subprocess call elements for nested flow
   *                        resolution.
   * @param taskPath        Additional metadata for the current task traversal.
   */
  private static void followNodes(String targetPid, Path path, SequenceFlow currentFlow,
      List<ProcessElement> subProcessCalls, TaskPath taskPath) {
    // If the path is already marked as found, stop traversal.
    if (path.isFound()) {
      return;
    }

    // Add the current flow's process element ID to the path.
    final String flowPid = ProcessUtils.getElementPid(currentFlow);
    path.getNodesInPath().add(flowPid);

    // Reached the target node? Stop traversal.
    if (flowPid.equals(targetPid)) {
      return;
    }

    // Get the process element at the destination of the current sequence flow.
    final ProcessElement destinationElement = (ProcessElement) currentFlow.getTarget();

    // If the destination element meets end-of-path conditions, stop traversal.
    if (isEndOfPathByGivenProcessElement(targetPid, path, destinationElement)) {
      return;
    }

    // Resolve the next process element to follow, given the path so far.
    ProcessElement nextElement = resolveNextElementForNode(path.getNodesInPath(), destinationElement, flowPid);

    // Special case: If at a CallSubEnd with no next element, try to get the nested
    // sub-element.
    if (nextElement == null && destinationElement instanceof CallSubEnd) {
      nextElement = getNestedSubElement(destinationElement, subProcessCalls);
    }

    // If next element is not the same as the destination, check for end-of-path
    // again and handle sub-to-sub connection.
    if (nextElement != destinationElement) {
      if (isEndOfPathByGivenProcessElement(targetPid, path, nextElement)) {
        return;
      }
      // If destination is an embedded end and next is embedded, handle direct
      // sub-to-sub navigation.
      if (ProcessUtils.isEmbeddedEndInstance(destinationElement)
          && ProcessUtils.isEmbeddedElementInstance(nextElement)) {
        nextElement = getNextElementForSubToSub(destinationElement, nextElement);
      }
    }

    // If the next element is a CallSubEnd, resolve the nested sub-element again.
    if (nextElement instanceof CallSubEnd) {
      nextElement = getNestedSubElement(nextElement, subProcessCalls);
    }

    // Retrieve all outgoing flows from the next element.
    List<SequenceFlow> nextOutgoingFlows = getNextOutgoingFlows(nextElement);
    final boolean isAlternative = ProcessUtils.isAlternativeInstance(nextElement);
    final String nextElementPid = ProcessUtils.getElementPid(nextElement);

    // If the next element is a decision/alternative or has multiple outgoing flows,
    // split the path.
    if (isAlternative || nextOutgoingFlows.size() > 1) {
      finishCurrentPathAndOpenNewPathForNextFlows(targetPid, path, subProcessCalls, taskPath, nextElementPid,
          nextOutgoingFlows);
      return;
    }

    // If the next element is an end node for the process path, mark the path as not
    // found and set end ID.
    if (ProcessUtils.isProcessPathEndElement(nextElement)) {
      path.setStatus(PathStatus.NOT_FOUND);
      path.setEndPathId(nextElementPid);
      return;
    }

    // Continue recursion with the (only) outgoing flow.
    if (!nextOutgoingFlows.isEmpty()) {
      followNodes(targetPid, path, nextOutgoingFlows.get(0), subProcessCalls, taskPath);
    }
  }

  private static void finishCurrentPathAndOpenNewPathForNextFlows(String targetPid, Path path,
      List<ProcessElement> subProcessCalls, TaskPath taskPath, String nextElementPid,
      List<SequenceFlow> nextOutgoingFlows) {
    path.setStatus(PathStatus.FINISHED);
    path.setEndPathId(nextElementPid);

    for (var outgoing : nextOutgoingFlows) {
      var startNewPath = new Path(nextElementPid, new ArrayList<>());
      // Find existing path before adding - otherwise return
      if (taskPath.isFinished() || isVisitedPath(startNewPath, taskPath, outgoing)) {
        return;
      }
      taskPath.getPaths().add(startNewPath);
      followNodes(targetPid, startNewPath, outgoing, subProcessCalls, taskPath);
    }
  }

  private static boolean isVisitedPath(Path startNewPath, TaskPath taskPath, SequenceFlow firstOutgoing) {
    return taskPath.getPaths().stream().filter(path -> path.getStatus() != null)
        .filter(path -> path.getStartPathId().equals(startNewPath.getStartPathId()))
        .anyMatch(path -> path.getNodesInPath().getFirst().equals(ProcessUtils.getElementPid(firstOutgoing)));
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
      final String startPathId) {
    if (CollectionUtils.isEmpty(paths)) {
      return;
    }

    paths.stream().filter(path -> path.getEndPathId().equals(startPathId)).findFirst().ifPresent(path -> {
      nodesInPath.addAll(0, path.getNodesInPath());
      var remainingPaths = paths.stream()
          .filter(availablePath -> !availablePath.getStartPathId().equals(path.getStartPathId())).toList();
      collectNodeIdsFromStartPathIdInFoundPaths(remainingPaths, nodesInPath, path.getStartPathId());
    });
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
    if (StringUtils.equals(processElementId, targetProcessElementId)) {
      path.setStatus(PathStatus.FOUND);
      path.setEndPathId(processElementId);
      return true;
    } else if (ProcessUtils.isTaskSwitchInstance(processElement) || ProcessUtils.isTaskEndInstance(processElement)) {
      path.setStatus(PathStatus.NOT_FOUND);
      path.setEndPathId(processElementId);
      return true;
    } else if (isEndOfLoop(processElementId, path)) {
      path.setStatus(PathStatus.END_LOOP);
      path.setEndPathId(processElementId);
      return true;
    }
    return false;
  }

  private static boolean isEndOfLoop(String processElementId, Path path) {
    return StringUtils.equalsAnyIgnoreCase(processElementId, path.getStartPathId());
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
