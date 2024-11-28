package com.axonivy.utils.process.analyser.test.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyser.internal.ProcessUtils;
import com.axonivy.utils.process.analyser.test.BaseSetup;

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
  void test_extractAlterNativeElementsWithMultiOutGoing() {
    var results = ProcessUtils.extractAlterNativeElementsWithMultiOutGoing(testProcessElements);
    assertThat(results).isNotEmpty();
    assertThat(results.size()).isEqualTo(1);
  }

  @Test
  void test_isIWebStartableNeedToRecordStatistic() {
    assertThat(ProcessUtils.isIWebStartableNeedToRecordStatistic(testProcessStart)).isTrue();
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
    assertThat(results.size()).isEqualTo(1);
  }

  @Test
  void test_getProcessElementsFromIProcessWebStartable() {
    assertThat(ProcessUtils.getProcessElementsFromIProcessWebStartable(testProcessStart).size()).isEqualTo(5);
  }

  @Test
  void test_getNestedProcessElementsFromSub() {
    assertThat(ProcessUtils.getNestedProcessElementsFromSub(startProcessElement)).isEmpty();
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

}