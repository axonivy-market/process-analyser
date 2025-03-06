package com.axonivy.solutions.process.analyser.test;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;

import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;

@SuppressWarnings("restriction")
public class BaseSetup {
  protected static final String TEST_REQUEST_PATH = "193485C5ABDFEA93/193485C5ABDFEA93-f0/test.ivp";
  protected static final String TEST_PROCESS_ELEMENT_START_PID = "193485C5ABDFEA93-f0";
  protected static final String TEST_FLOW_PID_FROM_START_ELEMENT = "193485C5ABDFEA93-f2";
  protected static final String TEST_PROCESS_RAW_PID = "193485C5ABDFEA93";
  protected static final String TEST_PROCESS_NAME = "test.ivp";
  protected static final String TEST_MODULE_NAME = "process-analyser-test";
  protected static IProcessWebStartable testProcessStart;
  protected static ProcessElement startProcessElement;
  protected static List<ProcessElement> testProcessElements;
  protected static SequenceFlow flowFromStartElement;

  protected static void prepareData() {
    testProcessStart = (IProcessWebStartable) ProcessUtils.getAllProcesses().stream()
        .filter(start -> StringUtils.contains(start.getName(), TEST_PROCESS_NAME)).findAny().orElse(null);
    testProcessElements = ProcessUtils.getProcessElementsFromIProcessWebStartable(testProcessStart);
    startProcessElement = testProcessElements.stream()
        .filter(element -> StringUtils.contains(element.getPid().toString(), TEST_PROCESS_ELEMENT_START_PID)).findAny()
        .orElse(null);
    flowFromStartElement = startProcessElement.getOutgoing().get(0);
  }

  protected SequenceFlow getEndFlowFromAlternative() {
    return ((Alternative) flowFromStartElement.getTarget()).getOutgoing().stream()
        .filter(flow -> ProcessUtils.getElementPid(flow).contains("f8")).findAny().orElse(null);
  }

  protected NodeElement getElementNextToTestStart() {
    return flowFromStartElement.getTarget();
  }
}
