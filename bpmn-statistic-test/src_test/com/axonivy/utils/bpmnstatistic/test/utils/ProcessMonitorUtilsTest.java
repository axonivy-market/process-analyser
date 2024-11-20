package com.axonivy.utils.bpmnstatistic.test.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.bpmnstatistic.bo.Node;
import com.axonivy.utils.bpmnstatistic.enums.NodeType;
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;
import com.axonivy.utils.bpmnstatistic.utils.ProcessesMonitorUtils;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
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
}
