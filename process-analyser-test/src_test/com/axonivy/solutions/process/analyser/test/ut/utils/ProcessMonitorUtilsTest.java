package com.axonivy.solutions.process.analyser.test.ut.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
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
import com.axonivy.solutions.process.analyser.enums.KpiColor;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.enums.NodeType;
import com.axonivy.solutions.process.analyser.utils.ProcessesMonitorUtils;
import com.axonivy.solutions.process.analyser.test.BaseSetup;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.workflow.ICase;

@IvyTest
public class ProcessMonitorUtilsTest extends BaseSetup {

  private final static String NODE_A_ID = "A";
  private final static String NODE_B_ID = "B";

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
    assertThat(results.size()).isEqualTo(16);
    assertThat(results.get(0).getLabelValue()).isEqualTo("0");
  }

  @Test
  void test_updateFrequencyForNodes() {
    ICase mockCase = ICase.current();
    Node mockNode = new Node();
    assertThat(mockNode.getLabel()).isNull();
    List<Node> results = List.of(mockNode);
    ProcessesMonitorUtils.updateFrequencyForNodes(results, new ArrayList<>(),
        List.of(mockCase));
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
    ProcessesMonitorUtils.updateNodeWiwthDefinedFrequency(mockValue, mockNode);
    assertThat(mockNode.getLabelValue()).isEqualTo(String.valueOf(mockValue));
    assertThat(mockNode.getFrequency()).isEqualTo(mockValue);
    assertThat(mockNode.getRelativeValue()).isEqualTo(1);
  }

  @Test
  void test_getNonRunningElementIdsFromAlternativeEnds() {
    AlternativePath testPath = new AlternativePath();
    String mockPrecedingFlowId = "f1";
    List<String> mockNodeIds = List.of("f2", "f3");
    List<String> mockNonRunningElementIds = List.of(mockPrecedingFlowId);
    testPath.setPathFromAlternativeEnd(true);
    testPath.setNodeIdsInPath(mockNodeIds);
    testPath.setPrecedingFlowIds(mockNonRunningElementIds);
    List<String> results = ProcessesMonitorUtils.getNonRunningElementIdsFromAlternativeEnds(List.of(testPath),
        mockNonRunningElementIds);
    assertThat(results.size()).isEqualTo(2);
  }

  @Test
  void test_updateRelativeValueForNodes() {
    List<Node> nodes = prepareMockNodeList();
    ProcessesMonitorUtils.updateRelativeValueForNodes(nodes);
    assertThat(nodes.getFirst().getRelativeValue()).isEqualTo(0.5f);
  }

  @Test
  void test_getFrequencyById() {
    List<Node> nodes = prepareMockNodeList();
    assertThat(ProcessesMonitorUtils.getFrequencyById(NODE_A_ID, nodes)).isEqualTo(1);
    assertThat(ProcessesMonitorUtils.getFrequencyById(NODE_B_ID, nodes)).isEqualTo(2);
  }

  @Test
  void test_findNodeById() {
    List<Node> nodes = prepareMockNodeList();
    assertThat(ProcessesMonitorUtils.findNodeById(NODE_A_ID, nodes).getFrequency()).isEqualTo(1);
    assertThat(ProcessesMonitorUtils.findNodeById(NODE_B_ID, nodes).getFrequency()).isEqualTo(2);
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

  @Test
  void test_countFrequencyOfTask() {
    List<String> taskIdsDoneInCase = List.of(NODE_A_ID, NODE_B_ID, NODE_A_ID, NODE_A_ID, NODE_B_ID);
    Map<String, Integer> idCountMap = ProcessesMonitorUtils.countFrequencyOfTask(taskIdsDoneInCase);
    assertThat(idCountMap.get(NODE_A_ID).intValue()).isEqualTo(3);
    assertThat(idCountMap.get(NODE_B_ID).intValue()).isEqualTo(2);
  }

  @Test
  void test_generateColorSegments_withFrequency_returnsEnumPalette() {
    List<String> expected = KpiColor.FREQUENCY.getColors();
    List<String> actual = ProcessesMonitorUtils.generateColorSegments(KpiType.FREQUENCY);
    assertThat(actual).containsExactlyElementsOf(expected);
  }

  @Test
  void test_generateColorSegments_withDuration_returnsEnumPalette() {
    List<String> expected = KpiColor.DURATION.getColors();
    List<String> actual = ProcessesMonitorUtils.generateColorSegments(KpiType.DURATION);
    assertThat(actual).containsExactlyElementsOf(expected);
  }

  @Test
  void test_generateGradientFromRgb_withDarkColor() {
    List<String> gradient = ProcessesMonitorUtils.generateGradientFromRgb("rgb(20, 40, 60)", 5);
    assertThat(gradient).hasSize(5);
    assertThat(gradient.get(0)).startsWith("rgb(");
    assertThat(gradient.get(0)).isNotEqualTo(gradient.get(4));
  }

  @Test
  void test_generateGradientFromRgb_withLightColor() {
    List<String> gradient = ProcessesMonitorUtils.generateGradientFromRgb("rgb(250, 250, 250)", 4);
    assertThat(gradient).hasSize(4);
    assertThat(gradient.get(0)).isNotEqualTo(gradient.get(3));
    assertThat(gradient.get(3)).isEqualTo("rgb(250, 250, 250)");
  }

  @Test
  void test_generateGradientFromRgb_invalidFormat_throwsException() {
    String invalidInput = "rgba(255, 255, 255)";
    assertThatThrownBy(() -> ProcessesMonitorUtils.generateGradientFromRgb(invalidInput, 5))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid RGB format");
  }

  @Test
  void test_generateGradientFromRgb_oneStep() {
    List<String> gradient = ProcessesMonitorUtils.generateGradientFromRgb("rgb(120, 130, 140)", 1);
    assertThat(gradient).hasSize(1);
    assertThat(gradient.get(0)).startsWith("rgb(");
  }

  @Test
  void test_generateGradientFromRgb_darkColorGetsBrighter() {
    String input = "rgb(10, 20, 30)";
    int steps = 5;
    List<String> gradient = ProcessesMonitorUtils.generateGradientFromRgb(input, steps);
    assertThat(gradient).hasSize(steps);

    // Extract and compare RGB values: brightness should increase
    for (int i = 1; i < steps; i++) {
      int prev = extractBrightness(gradient.get(i - 1));
      int curr = extractBrightness(gradient.get(i));
      assertThat(curr).isGreaterThanOrEqualTo(prev);
    }
  }

  @Test
  void test_generateGradientFromRgb_lightColorGetsDarker() {
    String input = "rgb(240, 240, 240)";
    int steps = 6;
    List<String> gradient = ProcessesMonitorUtils.generateGradientFromRgb(input, steps);
    assertThat(gradient).hasSize(steps);
    for (int i = 1; i < steps; i++) {
      int prev = extractBrightness(gradient.get(i - 1));
      int curr = extractBrightness(gradient.get(i));
      assertThat(curr).isLessThanOrEqualTo(prev);
    }
  }

  @Test
  void test_generateGradientFromRgb_middleGray_brightensTowardWhite() {
    String input = "rgb(128, 128, 128)";
    int steps = 4;
    List<String> gradient = ProcessesMonitorUtils.generateGradientFromRgb(input, steps);
    assertThat(gradient.get(0)).isNotEqualTo(gradient.get(steps - 1));
    assertThat(extractBrightness(gradient.get(steps - 1))).isGreaterThan(extractBrightness(gradient.get(0)));
  }

  @Test
  void test_generateGradientFromRgb_minRgb_becomesBrighterWhite() {
    String input = "rgb(0, 0, 0)";
    int steps = 3;
    List<String> gradient = ProcessesMonitorUtils.generateGradientFromRgb(input, steps);
    assertThat(gradient.get(0)).isEqualTo("rgb(216, 216, 216)"); // depends on brighten factor
    assertThat(gradient.get(steps - 1)).isEqualTo("rgb(0, 0, 0)");
  }

  @Test
  void test_generateGradientFromRgb_maxRgb_becomesDarkerBlack() {
    String input = "rgb(255, 255, 255)";
    int steps = 3;
    List<String> gradient = ProcessesMonitorUtils.generateGradientFromRgb(input, steps);
    assertThat(gradient.get(0)).startsWith("rgb(3"); // Very dark
    assertThat(gradient.get(steps - 1)).isEqualTo("rgb(255, 255, 255)");
  }

  // Helper method to calculate perceived brightness from rgb string
  private int extractBrightness(String rgbString) {
    String[] parts = rgbString.replaceAll("[^0-9,]", "").split(",");
    int r = Integer.parseInt(parts[0].trim());
    int g = Integer.parseInt(parts[1].trim());
    int b = Integer.parseInt(parts[2].trim());
    return (int) (0.299 * r + 0.587 * g + 0.114 * b);
  }
}
