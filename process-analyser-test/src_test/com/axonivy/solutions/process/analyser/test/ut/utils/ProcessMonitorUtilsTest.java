package com.axonivy.solutions.process.analyser.test.ut.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.primefaces.model.TreeNode;

import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.ProcessAnalyser;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.bo.StartElement;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.resolver.NodeResolver;
import com.axonivy.solutions.process.analyser.test.BaseSetup;
import com.axonivy.solutions.process.analyser.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;

@IvyTest
public class ProcessMonitorUtilsTest extends BaseSetup {

  private ProcessAnalyser processAnalyser;

  @BeforeAll
  static void setUp() {
    prepareData();
  }

  @Test
  void test_convertSequenceFlowToNode() {
    Node result = NodeResolver.convertSequenceFlowToNode(flowFromStartElement);
    assertThat(result.getId()).isEqualTo(TEST_FLOW_PID_FROM_START_ELEMENT);
    assertThat(result.getLabel()).isEmpty();
    assertThat(result.getType()).isEqualTo(NodeType.ARROW);
    assertThat(result.getFrequency()).isZero();
  }

  @Test
  void test_extractNodesFromProcessElements() {
    List<Node> results = NodeResolver.convertToNodes(List.of(startProcessElement), testSequenceFlows);
    assertThat(results.size()).isNotZero();
    assertThat(results.get(0).getId()).isEqualTo(TEST_PROCESS_ELEMENT_START_PID);
  }

  @Test
  void test_convertProcessElementToNode() {
    Node result = NodeResolver.convertProcessElementToNode(startProcessElement).getFirst();
    assertThat(result.getId()).isEqualTo(TEST_PROCESS_ELEMENT_START_PID);
    assertThat(result.getLabel()).isEqualTo("test");
    assertThat(result.getType()).isEqualTo(NodeType.ELEMENT);
    assertThat(result.getFrequency()).isZero();
  }

  @Test
  void test_updateNodeByAnalysisType() {
    Node result = new Node();
    result.setFrequency(10);
    result.setMedianDuration(2);
    assertThat(result.getLabel()).isNull();
    NodeResolver.updateNodeByAnalysisType(result, KpiType.FREQUENCY);
    assertThat(result.getLabelValue()).isEqualTo("10");
    assertThat(result.getRelativeValue()).isZero();
    NodeResolver.updateNodeByAnalysisType(result, KpiType.DURATION_OVERALL);
    assertThat(result.getLabelValue()).isEqualTo("2s");
  }

  @Test
  void test_filterInitialStatisticByIntervalTime() {
    String selectedPid = testProcess.getId();
    prepareProcessAnalyzer();
    List<ICase> cases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(
        ProcessUtils.getTaskStartIdFromPID(selectedPid), new TimeIntervalFilter(new Date(), new Date()),
        new ArrayList<>(), false);
    List<ITask> tasks = cases.stream().flatMap(ivyCase -> ivyCase.tasks().all().stream()).toList();
    List<Node> results = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(processAnalyser, KpiType.FREQUENCY, tasks);
    assertThat(results.size()).isEqualTo(24);
    assertThat(results.get(0).getLabelValue()).isEqualTo("0");
  }

  @Test
  void test_filterInitialStatisticByIntervalTime_withMergeToggle() {
    processAnalyser = new ProcessAnalyser();
    processAnalyser.setProcess(testProcess);
    processAnalyser.setStartElement(null);
    List<ICase> allCases = new ArrayList<>();
    for (StartElement startElement : testProcess.getStartElements()) {
      allCases.addAll(ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(startElement.getTaskStartId(),
          new TimeIntervalFilter(new Date(), new Date()), new ArrayList<>(), false));
    }

    List<ITask> tasks = allCases.stream().flatMap(ivyCase -> ivyCase.tasks().all().stream()).toList();
    List<Node> results = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(
      processAnalyser, KpiType.FREQUENCY, tasks);

    assertThat(results).isNotEmpty();
    assertThat(results.size()).isGreaterThanOrEqualTo(24);
  }

  @Test
  void test_filterInitialStatisticByIntervalTime_duration_singleStart() {
    String selectedPid = testProcess.getId();
    prepareProcessAnalyzer();
    List<ICase> cases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(
        ProcessUtils.getTaskStartIdFromPID(selectedPid), new TimeIntervalFilter(new Date(), new Date()),
        new ArrayList<>(), false);
    List<ITask> tasks = cases.stream().flatMap(ivyCase -> ivyCase.tasks().all().stream()).toList();
    List<Node> results = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(processAnalyser, KpiType.DURATION_OVERALL, tasks);
    assertThat(results).isNotEmpty();
    assertThat(results.get(0).getLabelValue()).endsWith("s");
  }

  @Test
  void test_filterInitialStatisticByIntervalTime_duration_merged() {
    processAnalyser = new ProcessAnalyser();
    processAnalyser.setProcess(testProcess);
    processAnalyser.setStartElement(null);

    List<ICase> allCases = new ArrayList<>();
    for (StartElement startElement : testProcess.getStartElements()) {
      allCases.addAll(ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(startElement.getTaskStartId(),
          new TimeIntervalFilter(new Date(), new Date()), new ArrayList<>(), false));
    }
    List<ITask> tasks = allCases.stream().flatMap(ivyCase -> ivyCase.tasks().all().stream()).toList();
    List<Node> results = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(processAnalyser, KpiType.DURATION_OVERALL, tasks);
    assertThat(results).isNotEmpty();
    assertThat(results.get(0).getLabelValue()).endsWith("s");
  }

  @Test
  void test_getAllCasesFromTaskStartIdWithTimeInterval() {
    List<ICase> results = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(0L,
        new TimeIntervalFilter(new Date(), new Date()), new ArrayList<CustomFieldFilter>(), false);
    assertThat(results.size()).isZero();
  }

  @Test
  void test_buildTreeFromNodes_withEmptyList() {
    TreeNode<Object> result = ProcessesMonitorUtils.buildTreeFromNodes(new ArrayList<>());
    assertThat(result).isNotNull();
    assertThat(result.getChildren()).isEmpty();
  }

  @Test
  void test_buildTreeFromNodes_withSingleNode() {
    Node node = new Node();
    node.setId("node1");
    node.setLabel("Node 1");
    node.setParentNodeId(null);

    List<Node> nodes = List.of(node);
    TreeNode<Object> result = ProcessesMonitorUtils.buildTreeFromNodes(nodes);

    assertThat(result.getChildren()).hasSize(1);
    TreeNode<Object> childNode = result.getChildren().get(0);
    assertThat(childNode.getData()).isEqualTo(node);
    assertThat(childNode.isExpanded()).isTrue();
  }

  @Test
  void test_buildTreeFromNodes_withMultipleLevels() {
    Node grandParent = new Node();
    grandParent.setId("gp");
    grandParent.setLabel("Grand Parent");
    grandParent.setParentNodeId(null);

    Node parent = new Node();
    parent.setId("p");
    parent.setLabel("Parent");
    parent.setParentNodeId("gp");

    Node child = new Node();
    child.setId("c");
    child.setLabel("Child");
    child.setParentNodeId("p");

    List<Node> nodes = List.of(grandParent, parent, child);
    TreeNode<Object> result = ProcessesMonitorUtils.buildTreeFromNodes(nodes);

    assertThat(result.getChildren()).hasSize(1);
    TreeNode<Object> gpNode = result.getChildren().get(0);
    assertThat(gpNode.getData()).isEqualTo(grandParent);
    assertThat(gpNode.getChildren()).hasSize(1);
    TreeNode<Object> pNode = gpNode.getChildren().get(0);
    assertThat(pNode.getData()).isEqualTo(parent);
    assertThat(pNode.getChildren()).hasSize(1);
    TreeNode<Object> cNode = pNode.getChildren().get(0);
    assertThat(cNode.getData()).isEqualTo(child);
  }

  private void prepareProcessAnalyzer() {
    processAnalyser = new ProcessAnalyser();
    processAnalyser.setProcess(new Process());
    processAnalyser.setStartElement(new StartElement());
    processAnalyser.getStartElement().setPid(testProcess.getId());
    processAnalyser.setProcess(testProcess);
  }
}
