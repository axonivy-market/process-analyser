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
  protected static final String TEST_APPLICATION_NAME = "test";
  protected static final String TEST_MODULE_NAME = "process-analyser-test";
  protected static final String SELECTED_STARTABLE_ID = "SupportHR.p.json";
  protected static final String TEST_IFRAME_SOURCE_URL = "/test/faces/view/process-analyser-test/process-miner.xhtml?server=localhost:8080&app=test&pmv=HRTest&file=/processes/"
      + SELECTED_STARTABLE_ID;
  protected static final String SELECTED_MODULE_URL = "HRTest";
  protected static final String OUTER_FLOW_TO_SUB_PID = "193485C5ABDFEA93-f16";
  protected static final String SUB_PROCESS_START = "193485C5ABDFEA93-S10-g0";
  protected static final String SUB_PROCESS_END = "193485C5ABDFEA93-S10-g1";
  protected static final String EMBEDDED_PID = "193485C5ABDFEA93-S10";
  protected static final String REST_CALL_PID = "193485C5ABDFEA93-f3";

  protected static IProcessWebStartable testProcessStart;
  protected static ProcessElement startProcessElement;
  protected static List<ProcessElement> testProcessElements;
  protected static SequenceFlow flowFromStartElement;
  protected static List<SequenceFlow> testSequenceFlows;
  protected static ProcessElement subProcessElement;
  protected static ProcessElement subProcessCall;
  protected static String outerFlowPid;
  protected static ProcessElement embeddedStart;
  protected static ProcessElement embeddedEnd;

  protected static void prepareData() {
    testProcessStart = (IProcessWebStartable) ProcessUtils.getAllProcesses().stream()
        .filter(start -> StringUtils.contains(start.getName(), TEST_PROCESS_NAME)).findAny().orElse(null);
    testProcessElements = ProcessUtils.getProcessElementsFrom(testProcessStart);
    subProcessElement = getProcessElementByPid(EMBEDDED_PID);
    subProcessCall = getProcessElementByPid(REST_CALL_PID);
    testSequenceFlows = ProcessUtils.getSequenceFlowsFrom(testProcessElements);
    startProcessElement = getProcessElementByPid(TEST_PROCESS_ELEMENT_START_PID);
    outerFlowPid = testSequenceFlows.stream().filter(arrow -> OUTER_FLOW_TO_SUB_PID.equals(arrow.getPid().toString()))
        .map(flow -> flow.getPid().toString()).findAny().get();
    embeddedStart = getProcessElementByPid(SUB_PROCESS_START);
    embeddedEnd = getProcessElementByPid(SUB_PROCESS_END);
    flowFromStartElement = startProcessElement.getOutgoing().get(0);
  }

  protected SequenceFlow getEndFlowFromAlternative() {
    return ((Alternative) flowFromStartElement.getTarget()).getOutgoing().stream()
        .filter(flow -> ProcessUtils.getElementPid(flow).contains("f8")).findAny().orElse(null);
  }

  protected NodeElement getElementNextToTestStart() {
    return flowFromStartElement.getTarget();
  }

  private static ProcessElement getProcessElementByPid(String pid) {
    return testProcessElements.stream().filter(element -> pid.equals(element.getPid().toString())).findAny().get();
  }
}
