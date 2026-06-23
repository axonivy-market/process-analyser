package com.axonivy.solutions.process.analyser.demo.managedbean;

import java.util.ArrayList;
import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.eventstart.AbstractProcessStartEventBean;
import ch.ivyteam.ivy.workflow.query.CaseQuery;

public class PrepareDataDemoBean extends AbstractProcessStartEventBean {
  private static final int SIGNAL_SEND_REPEAT = 5;
  private static final List<String> SIGNAL_LIST = List.of("hotelBooking", "bookFlight", "flightPayment", "planFlight", "recommendDestination",
      "mortgage:purchase", "initiateDataFromQueue");

  public PrepareDataDemoBean() {
    super("PrepareDataDemoBean", "Description of PrepareDataDemoBean");
  }

  @Override
  public void poll() {
    boolean hasData = isDemoDataAlreadyPersisted();
    if (!hasData) {
      sendSignals();
    }
    getEventBeanRuntime().poll().disable();
  }

  private boolean isDemoDataAlreadyPersisted() {
    List<Long> taskStartIds = new ArrayList<>();
//    List<IProcess> demoProcesses =  ProcessUtils.getProcessesInCurrentPMV(Ivy.request().getProcessModelVersion());
    CaseQuery caseQuery = CaseQuery.create();
    caseQuery.where().taskStartId().isIn(taskStartIds);
    return false;
  }

  public static void sendSignals() {
    for (int i = 0; i < SIGNAL_SEND_REPEAT; i++) {
      for (String signal : SIGNAL_LIST) {
        Ivy.wf().signals().create().send(signal);
      }
    }
  }
}