package com.axonivy.solutions.process.analyser.test.ut.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.solutions.process.analyser.bo.AlternativePath;
import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.test.BaseSetup;
import com.axonivy.solutions.process.analyser.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.workflow.ICase;

@IvyTest
public class ProcessMonitorUtilsTest extends BaseSetup {

  private final static String NODE_A_ID = "A";
  private final static String NODE_B_ID = "B";
  private final static String NODE_C_ID = "C";
  private final static String TASK_A_ID = "TASK_A";
  private final static String TASK_B_ID = "TASK_B";

  @BeforeAll
  static void setUp() {
    prepareData();
  }

  @Test
  void test_convertSequenceFlowToNode() {
    Node result = ProcessesMonitorUtils.convertSequenceFlowToNode(flowFromStartElement);
    assertThat(result.getId()).isEqualTo(TEST_FLOW_PID_FROM_START_ELEMENT);
    assertThat(result.getLabel()).isEmpty();
    assertThat(result.getType()).isEqualTo(NodeType.ARROW);
    assertThat(result.getFrequency()).isZero();
  }

  @Test
  void test_extractNodesFromProcessElements() {
    List<Node> results = ProcessesMonitorUtils.convertToNodes(List.of(startProcessElement), testSequenceFlows);
    assertThat(results.size()).isNotZero();
    assertThat(results.get(0).getId()).isEqualTo(TEST_PROCESS_ELEMENT_START_PID);
  }

  @Test
  void test_convertProcessElementToNode() {
    Node result = ProcessesMonitorUtils.convertProcessElementToNode(startProcessElement).getFirst();
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
    ProcessesMonitorUtils.updateNodeByAnalysisType(result, KpiType.FREQUENCY);
    assertThat(result.getLabelValue()).isEqualTo("10");
    assertThat(result.getRelativeValue()).isZero();
    ProcessesMonitorUtils.updateNodeByAnalysisType(result, KpiType.DURATION_OVERALL);
    assertThat(result.getLabelValue()).isEqualTo("2s");
  }

  @Test
  void test_filterInitialStatisticByIntervalTime() {
    String selectedPid = testProcessStart.pid().getParent().toString();
    List<ICase> cases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(
        ProcessUtils.getTaskStartIdFromPID(selectedPid), new TimeIntervalFilter(new Date(), new Date()), new ArrayList<>());
    List<Node> results = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(testProcessStart, KpiType.FREQUENCY, cases);
    assertThat(results.size()).isEqualTo(28);
    assertThat(results.get(0).getLabelValue()).isEqualTo("0");
  }

  @Test
  void test_updateFrequencyForNodes() {
    ICase mockCase = ICase.current();
    Node mockNode = new Node();
    assertThat(mockNode.getLabel()).isNull();
    List<Node> results = List.of(mockNode);
    ProcessesMonitorUtils.updateFrequencyForNodes(results, new ArrayList<>(), List.of(mockCase));
    assertThat(results.size()).isNotZero();
    assertThat(results.get(0).getLabelValue()).isEqualTo("1");
  }

  @Test
  void test_followPath() {
    AlternativePath testPath = new AlternativePath();
    var flowFromAlternative = getEndFlowFromAlternative();
    testPath.setNodeIdsInPath(new ArrayList<>());
    ProcessesMonitorUtils.followPath(testPath, flowFromAlternative);
    assertThat(testPath.getNodeIdsInPath().size()).isEqualTo(1);
    assertThat(testPath.getTaskSwitchEventIdOnPath()).isNullOrEmpty();
  }

  @Test
  void test_getAllCasesFromTaskStartIdWithTimeInterval() {
    List<ICase> results = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(0L,
        new TimeIntervalFilter(new Date(), new Date()), new ArrayList<CustomFieldFilter>());
    assertThat(results.size()).isZero();
  }

  @Test
  void test_updateNodeWiwthDefinedFrequency() {
    Node mockNode = new Node();
    int mockValue = 9;
    ProcessesMonitorUtils.updateNodeWithDefinedFrequency(mockValue, mockNode);
    assertThat(mockNode.getLabelValue()).isEqualTo(String.valueOf(mockValue));
    assertThat(mockNode.getFrequency()).isEqualTo(mockValue);
    assertThat(mockNode.getRelativeValue()).isEqualTo(1);
  }

  @Test
  void test_updateRelativeValueForNodes() {
    List<Node> nodes = prepareMockNodeList();
    ProcessesMonitorUtils.updateRelativeValueForNodes(nodes);
    assertThat(nodes.getFirst().getRelativeValue()).isEqualTo(0.5f);
  }

  @Test
  void test_updateFrequencyForComplexElements() {
    List<Node> nodes = prepareMockNodeList();
    Node nodeC = new Node();
    nodeC.setFrequency(0);
    nodeC.setId(NODE_C_ID);
    nodes.add(nodeC);
    AlternativePath alternative = new AlternativePath();
    alternative.setPrecedingFlowIds(List.of(NODE_A_ID,NODE_B_ID));
    alternative.setSolePathFromAlternativeEnd(true);
    alternative.setNodeIdsInPath(List.of(NODE_C_ID));
    ProcessesMonitorUtils.updateFrequencyForComplexElements(List.of(alternative), nodes);
    assertThat(nodes.getLast().getFrequency()).isEqualTo(3);
  }

  @Test
  void test_buildNodeWithTaskMap() {
    AlternativePath path1 = new AlternativePath();
    path1.setNodeIdsInPath(List.of(NODE_A_ID));
    path1.setTaskSwitchEventIdOnPath(TASK_A_ID);

    AlternativePath path2 = new AlternativePath();
    path2.setNodeIdsInPath(List.of(NODE_B_ID, NODE_C_ID));
    path2.setTaskSwitchEventIdOnPath(TASK_B_ID);

    Map<String, String> result = ProcessesMonitorUtils.buildNodeWithTaskMap(List.of(path1, path2), Collections.emptyMap());
    assertThat(result.size()).isEqualTo(3);
    assertThat(result.get(NODE_A_ID)).isEqualTo(TASK_A_ID);
    assertThat(result.get(NODE_B_ID)).isEqualTo(TASK_B_ID);
    assertThat(result.get(NODE_C_ID)).isEqualTo(TASK_B_ID);
  }

  private List<Node> prepareMockNodeList() {
    Node nodeA = new Node();
    nodeA.setFrequency(1);
    nodeA.setId(NODE_A_ID);
    Node nodeB = new Node();
    nodeB.setFrequency(2);
    nodeB.setId(NODE_B_ID);
    return List.of(nodeA, nodeB);
  }
}
