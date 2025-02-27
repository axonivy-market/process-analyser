package com.axonivy.solutions.process.analyser.test.ut.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.solutions.process.analyser.bo.AlternativePath;
import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.Node;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.utils.ProcessesMonitorUtils;
import com.axonivy.solutions.process.analyser.test.BaseSetup;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.workflow.ICase;

@IvyTest
public class ProcessMonitorUtilsTest extends BaseSetup {

  @BeforeAll
  static void setUp() {
    prepareData();
  }

  @Test
  void test_convertProcessElementInfoToNode() {
    List<Node> results = ProcessesMonitorUtils.convertProcessElementInfoToNode(startProcessElement);
    assertThat(results.size()).isEqualTo(1);
    assertThat(results.get(0).getId()).isEqualTo(TEST_FLOW_PID_FROM_START_ELEMENT);
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
    List<Node> results = new ArrayList<>();
    ProcessesMonitorUtils.extractNodesFromProcessElements(List.of(startProcessElement), results);
    assertThat(results.size()).isNotZero();
    assertThat(results.get(0).getId()).isEqualTo(TEST_PROCESS_ELEMENT_START_PID);
  }

  @Test
  void test_convertProcessElementToNode() {
    Node result = ProcessesMonitorUtils.convertProcessElementToNode(startProcessElement);
    assertThat(result.getId()).isEqualTo(TEST_PROCESS_ELEMENT_START_PID);
    assertThat(result.getLabel()).isEqualTo("test");
    assertThat(result.getType()).isEqualTo(NodeType.ELEMENT);
    assertThat(result.getFrequency()).isZero();
  }

  @Test
  void test_updateNodeByAnalysisType() {
    Node result = new Node();
    result.setFrequency(10);
    result.setMedianDuration(11.5);
    assertThat(result.getLabel()).isNull();
    ProcessesMonitorUtils.updateNodeByAnalysisType(result, KpiType.FREQUENCY);
    assertThat(result.getLabelValue()).isEqualTo(10);
    assertThat(result.getRelativeValue()).isZero();
    ProcessesMonitorUtils.updateNodeByAnalysisType(result, KpiType.DURATION);
    assertThat(result.getLabelValue()).isEqualTo(12);
  }

  @Test
  void test_filterInitialStatisticByIntervalTime() {
    String selectedPid = testProcessStart.pid().getParent().toString();
    List<ICase> cases = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(
      ProcessUtils.getTaskStartIdFromPID(selectedPid), new TimeIntervalFilter(new Date(), new Date()), new ArrayList<>());
    List<Node> results = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(testProcessStart, KpiType.FREQUENCY, cases);
    assertThat(results.size()).isEqualTo(10);
    assertThat(results.get(0).getLabelValue()).isZero();
  }

  @Test
  void test_updateFrequencyForNodes() {
    ICase mockCase = ICase.current();
    Node mockNode = new Node();
    assertThat(mockNode.getLabel()).isNull();
    List<Node> results = ProcessesMonitorUtils.updateFrequencyForNodes(List.of(mockNode), new ArrayList<>(),
        List.of(mockCase));
    assertThat(results.size()).isNotZero();
    assertThat(results.get(0).getLabelValue()).isEqualTo(1);
  }

  @Test
  void test_followPath() {
    AlternativePath testPath = new AlternativePath();
    var flowFromAlternative = getFirstFlowFromAlternative();
    testPath.setOriginFlow(flowFromAlternative);
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
    ProcessesMonitorUtils.updateNodeWiwthDefinedFrequency(mockValue, mockNode);
    assertThat(mockNode.getLabelValue()).isEqualTo(mockValue);
    assertThat(mockNode.getFrequency()).isEqualTo(mockValue);
    assertThat(mockNode.getRelativeValue()).isEqualTo(1);
  }
}
