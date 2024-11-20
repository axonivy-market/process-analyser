package com.axonivy.utils.bpmnstatistic.test.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.bpmnstatistic.bo.AlternativePath;
import com.axonivy.utils.bpmnstatistic.bo.Node;
import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.enums.KpiType;
import com.axonivy.utils.bpmnstatistic.enums.NodeType;
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;
import com.axonivy.utils.bpmnstatistic.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;

@IvyTest
@SuppressWarnings("restriction")
public class ProcessMonitorUtilsTest {
  private static IProcessWebStartable testProcessStart;
  private static final String TEST_PROCESS_NAME = "test.ivp";
  private static final String TEST_PROCESS_ELEMENT_START_PID = "193485C5ABDFEA93-f0";
  private static final String TEST_FLOW_PID_FROM_START_ELEMENT = "193485C5ABDFEA93-f2";
  private static ProcessElement startProcessElement;
  private static SequenceFlow flowFromStartElement;

  @BeforeAll
  static void setUp() {
    testProcessStart = (IProcessWebStartable) ProcessUtils.getAllProcesses().stream()
        .filter(start -> StringUtils.contains(start.getName(), TEST_PROCESS_NAME)).findAny().orElse(null);
    startProcessElement = ProcessUtils.getProcessElementsFromIProcessWebStartable(testProcessStart).stream()
        .filter(element -> StringUtils.contains(element.getPid().toString(), TEST_PROCESS_ELEMENT_START_PID)).findAny()
        .orElse(null);
    flowFromStartElement = startProcessElement.getOutgoing().get(0);
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
    List<Node> results = ProcessesMonitorUtils.filterInitialStatisticByIntervalTime(testProcessStart,
        new TimeIntervalFilter(new Date(), new Date()), KpiType.FREQUENCY);
    assertThat(results.size()).isEqualTo(10);
    assertThat(results.get(0).getLabelValue()).isZero();
  }

  @Test
  void test_updateFrequencyForNodes() {
    ICase mockCase = ICase.current();
    Node mockNode = new Node();
    assertThat(mockNode.getLabel()).isNull();
    List<Node> results = ProcessesMonitorUtils.updateFrequencyForNodes(List.of(mockNode),
        new ArrayList<ProcessElement>(), List.of(mockCase));
    assertThat(results.size()).isNotZero();
    assertThat(results.get(0).getLabelValue()).isEqualTo(1);
  }

  @Test
  void test_followPath() {
    AlternativePath testPath = new AlternativePath();
    SequenceFlow flowFromAlternative = ((Alternative) flowFromStartElement.getTarget()).getOutgoing().get(0);
    testPath.setOriginFlow(flowFromAlternative);
    testPath.setNodeIdsInPath(new ArrayList<>());
    ProcessesMonitorUtils.followPath(testPath, flowFromAlternative);
    assertThat(testPath.getNodeIdsInPath().size()).isEqualTo(3);
    assertThat(testPath.getTaskSwitchEventIdOnPath()).isNullOrEmpty();
  }

  @Test
  void test_getAllCasesFromTaskStartIdWithTimeInterval() {
    List<ICase> results = ProcessesMonitorUtils.getAllCasesFromTaskStartIdWithTimeInterval(0L,
        new TimeIntervalFilter(new Date(), new Date()));
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
