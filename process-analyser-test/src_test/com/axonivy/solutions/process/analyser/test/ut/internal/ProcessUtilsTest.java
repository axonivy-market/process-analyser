package com.axonivy.solutions.process.analyser.test.ut.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.test.BaseSetup;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class ProcessUtilsTest extends BaseSetup {

  @BeforeAll
  static void setUp() {
    prepareData();
  }

  @Test
  void test_getTaskElementIdFromRequestPath() {
    String result = ProcessUtils.getTaskElementIdFromRequestPath(TEST_REQUEST_PATH);
    assertThat(result).isEqualTo(TEST_PROCESS_ELEMENT_START_PID);
    result = ProcessUtils.getTaskElementIdFromRequestPath(TEST_PROCESS_ELEMENT_START_PID);
    assertThat(result).isEmpty();
  }

  @Test
  void test_extractAlterNativeElementsWithMultiOutgoings() {
    var results = ProcessUtils.getAlterNativesWithMultiOutgoings(testProcessElements);
    assertThat(results).isNotEmpty();
    assertThat(results.size()).isEqualTo(1);
  }

  @Test
  void test_getProcessesWithPmv() {
    var results = ProcessUtils.getProcessesWithPmv();
    assertThat(results).isNotEmpty();
    assertThat(results.keySet().size()).isEqualTo(1);
    assertThat(results.keySet().iterator().next()).isEqualTo(TEST_MODULE_NAME);
  }

  @Test
  void test_getAllProcesses() {
    var results = ProcessUtils.getAllProcesses();
    assertThat(results).isNotEmpty();
    assertThat(results.size()).isGreaterThanOrEqualTo(1);
    results.forEach(process -> assertThat(process.getStartElements()).isNotEmpty());
  }

  @Test
  void test_getProcessElementsFromIProcessWebStartable() {
    assertThat(ProcessUtils.getProcessElementsFrom(testProcess.getId(), testProcess.getPmv()).size())
        .isEqualTo(14);
  }

  @Test
  void test_getNestedProcessElementsFromSub() {
    assertThat(ProcessUtils.getNestedProcessElementsFromSub(startProcessElement)).isEmpty();
    assertEquals(3, ProcessUtils.getNestedProcessElementsFromSub(subProcessElement).size());
    assertEquals(3, ProcessUtils.getNestedProcessElementsFromSub(subProcessCall).size());
  }

  @Test
  void test_getProcessPidFromElement() {
    assertThat(ProcessUtils.getProcessPidFromElement(TEST_PROCESS_ELEMENT_START_PID)).isEqualTo(TEST_PROCESS_RAW_PID);
  }

  @Test
  void test_getElementPid() {
    assertThat(ProcessUtils.getElementPid(startProcessElement)).isEqualTo(TEST_PROCESS_ELEMENT_START_PID);
  }

  @Test
  void test_isAlternativeInstance() {
    assertThat(ProcessUtils.isAlternativeInstance(getElementNextToTestStart())).isTrue();
  }

  @Test
  void test_getSelectedProcessFilePath() {
    assertThat(ProcessUtils.getSelectedProcessFilePath(SELECTED_STARTABLE_ID, SELECTED_MODULE_URL, TEST_APPLICATION_NAME))
        .isEqualTo(SELECTED_STARTABLE_ID);
  }

  @Test
  void test_getEmbeddedStartConnectToFlow() {
    assertTrue(ProcessUtils.isEmbeddedStartConnectToSequenceFlow(embeddedStart, outerFlowPid));
  }

  @Test
  void test_getStartElementFromSubProcessCall() {
    assertNotNull(ProcessUtils.getStartElementFromSubProcessCall(subProcessCall),
        "Sub process call should be filter from test process");
  }

  @Test
  void test_isEmbeddedEndInstance() {
    assertTrue(ProcessUtils.isEmbeddedEndInstance(embeddedEnd));
  }

  @Test
  void test_isComplexElementWithMultiIncomings() {
    assertTrue(ProcessUtils.isComplexElementWithMultiIncomings(multiIncomingsElement));
  }
}