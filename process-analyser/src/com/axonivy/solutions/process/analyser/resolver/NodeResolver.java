package com.axonivy.solutions.process.analyser.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.constants.AnalyserConstants;
import com.axonivy.solutions.process.analyser.core.constants.CoreConstants;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.utils.DateUtils;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.value.PID;

@SuppressWarnings("restriction")
public class NodeResolver {

  private NodeResolver( ) {
    throw new IllegalStateException("Utility class");
  }

  public static List<Node> convertToNodes(Collection<ProcessElement> processElements, Collection<SequenceFlow> sequenceFlows) {
    return Stream.concat(
          processElements.stream().flatMap(processElement -> convertProcessElementToNode(processElement).stream()),
          sequenceFlows.stream().map(NodeResolver::convertSequenceFlowToNode))
        .collect(Collectors.toList());
  }

  public static List<Node> updateNodeByAnalysisType(List<Node> nodes, KpiType analysisType) {
    if (CollectionUtils.isEmpty(nodes)) {
      return new ArrayList<>();
    }
    nodes.forEach(node -> updateNodeByAnalysisType(node, analysisType));
    return nodes;
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
      node.setRelativeValue(AnalyserConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    }
  }

  /**
   * Convert process element to Node base on its class type
   **/
  public static List<Node> convertProcessElementToNode(ProcessElement element) {
    if (element == null) {
      return List.of();
    }

    String parentNodeId = ProcessUtils.getElementPid(element.getParent());
    Node node = createNode(ProcessUtils.getElementPid(element), element.getName(), NodeType.ELEMENT, parentNodeId);
    List<String> outGoingPathIds = extractPidOfSequenceFlows(element.getOutgoing());
    node.setOutGoingPathIds(outGoingPathIds);

    return switch (element) {
    case TaskSwitchGateway taskSwitchGateway -> {
      node.setInCommingPathIds(extractPidOfSequenceFlows(taskSwitchGateway.getIncoming()));
      node.setTaskSwitchGateway(true);
      String elementId = ProcessUtils.getElementPid(taskSwitchGateway);
      List<Node> taskNodes = CollectionUtils.emptyIfNull(taskSwitchGateway.getAllTaskConfigs()).stream()
          .map(task -> {
            String taskId = elementId + CoreConstants.SLASH + task.identifier().getTaskIvpLinkName();
            return createNode(taskId, task.name().getRawMacro(), NodeType.ELEMENT);
          })
          .collect(Collectors.toList());
      taskNodes.add(0, node);
      yield taskNodes;
    }
    case RequestStart requestStart -> {
      node.setRequestPath(requestStart.getRequestPath().getLinkPath());
      yield List.of(node);
    }
    default -> {
      node.setInCommingPathIds(extractPidOfSequenceFlows(element.getIncoming()));
      yield List.of(node);
    }};
  }

  public static void updateRelativeValueForNodes(Collection<Node> nodes) {
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

  public static Node convertSequenceFlowToNode(SequenceFlow flow) {
    if (flow == null) {
      return null;
    }

    String parentNodeId = ProcessUtils.getElementPid(Optional.ofNullable(flow)
        .map(SequenceFlow::getSource)
        .map(NodeElement::getParent)
        .orElse(null));
    Node node = createNode(ProcessUtils.getElementPid(flow), flow.getName(), NodeType.ARROW, parentNodeId);
    node.setTargetNodeId(ProcessUtils.getElementPid(flow.getTarget()));
    node.setSourceNodeId(ProcessUtils.getElementPid(flow.getSource()));
    return node;
  }

  private static List<String> extractPidOfSequenceFlows(List<SequenceFlow> sequenceFlows) {
    return CollectionUtils.emptyIfNull(sequenceFlows).stream()
        .map(ProcessUtils::getElementPid)
        .toList();
  }

  private static Node createNode(String id, String label, NodeType type) {
    Node node = new Node();
    node.setId(id);
    node.setLabel(label);
    node.setType(type);
    return node;
  }

  private static Node createNode(String id, String label, NodeType type, String parentNodeId) {
    Node node = createNode(id, label, type);
    node.setParentNodeId(parentNodeId);
    return node;
  }
}
