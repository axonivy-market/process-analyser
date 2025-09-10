package com.axonivy.solutions.process.analyser.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.utils.DateUtils;
import com.axonivy.solutions.process.analyser.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;

@SuppressWarnings("restriction")
public class NodeResolver {

  public static List<Node> convertToNodes(List<ProcessElement> processElements, List<SequenceFlow> sequenceFlows) {
    return Stream.concat(processElements.stream()
        .flatMap(pe -> ProcessesMonitorUtils.convertProcessElementToNode(pe).stream()),
            sequenceFlows.stream().map(ProcessesMonitorUtils::convertSequenceFlowToNode))
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
      node.setRelativeValue(ProcessAnalyticsConstants.DEFAULT_INITIAL_STATISTIC_NUMBER);
    }
  }
}
