package com.axonivy.utils.bpmnstatistic.utils;

public class unfinishedUtils {

//  private static List<WorkflowProgress> handleExceptionCase(ProcessElement targetElement, Long caseId) {
//    List<WorkflowProgress> results = new ArrayList<>();
//    String elementId = getElementRawId(targetElement.getPid().toString());
//    Ivy.log().warn(targetElement.getPid());
//    Ivy.log().warn(elementId);
//    List<SequenceFlow> incomingFlow = targetElement.getIncoming();
//    // check id of embedded element
//    if (elementId.equals("S10-f1")) {
//      Ivy.log().warn("aloha");
//      incomingFlow.stream().forEach(flow -> results.addAll(handleFlowFromEmbeddedElement(flow, caseId)));
//    } else {
//      SequenceFlow flowFromEmbedded = incomingFlow.stream()
//          .filter(flow -> flow.getSource() instanceof EmbeddedProcessElement).findAny().orElse(null);
//      if (!Objects.isNull(flowFromEmbedded)) {
//        NodeElement embeddedNode = flowFromEmbedded.getSource();
//        EmbeddedEnd targetEmbeddedEnds = (EmbeddedEnd) ((EmbeddedProcessElement) embeddedNode).getEmbeddedProcess()
//            .getProcessElements().stream().filter(k -> k instanceof EmbeddedEnd).map(u -> (EmbeddedEnd) u)
//            .filter(
//                z -> z.getConnectedOuterProcessElement().getPid().toString().equals(targetElement.getPid().toString()))
//            .findAny().orElse(null);
//        if (targetEmbeddedEnds != null) {
//          results.addAll(
//              repo.findByInprogessArrowIdAndCaseId(getElementRawId(targetEmbeddedEnds.getPid().toString()), caseId));
//          WorkflowProgress workflowFromUnupdateEmbeddedStart = new WorkflowProgress();
//          workflowFromUnupdateEmbeddedStart
//              .setProcessRawPid(targetElement.getPid().toString().split(ProcessMonitorConstants.HYPHEN_SIGN)[0]);
//          workflowFromUnupdateEmbeddedStart.setArrowId(getElementRawId(flowFromEmbedded.getPid().toString()));
//          workflowFromUnupdateEmbeddedStart.setOriginElementId(getElementRawId(embeddedNode.getPid().toString()));
//          workflowFromUnupdateEmbeddedStart
//              .setTargetElementId(getElementRawId(flowFromEmbedded.getTarget().getPid().toString()));
//          workflowFromUnupdateEmbeddedStart.setCaseId(getCurrentCaseId());
//          workflowFromUnupdateEmbeddedStart.setCondition(flowFromEmbedded.getCondition());
//          workflowFromUnupdateEmbeddedStart.setStartTimeStamp(new Date());
//          results.add(workflowFromUnupdateEmbeddedStart);
//        }
//      }
////      EmbeddedProcessElement sourceElement = incomingFlow.stream().findAny()
//    }
//    return results;
//  }
//
//  private static List<WorkflowProgress> handleFlowFromEmbeddedElement(SequenceFlow flow, Long caseId) {
//    NodeElement sourceElement = flow.getSource();
//    if (sourceElement instanceof EmbeddedStart) {
//      EmbeddedStart embeddedStart = (EmbeddedStart) sourceElement;
//      String correspondingFlowIdFromOutside = embeddedStart.getConnectedOuterSequenceFlow().getPid().toString();
//      Ivy.log().fatal(correspondingFlowIdFromOutside);
//      List<WorkflowProgress> persistArrow = repo
//          .findByInprogessArrowIdAndCaseId(getElementRawId(correspondingFlowIdFromOutside), caseId);
//      if (CollectionUtils.isNotEmpty(persistArrow)) {
//        WorkflowProgress workflowFromUnupdateEmbeddedStart = new WorkflowProgress();
//        workflowFromUnupdateEmbeddedStart
//            .setProcessRawPid(correspondingFlowIdFromOutside.split(ProcessMonitorConstants.HYPHEN_SIGN)[0]);
//        workflowFromUnupdateEmbeddedStart.setArrowId(getElementRawId(flow.getPid().toString()));
//        workflowFromUnupdateEmbeddedStart.setOriginElementId(getElementRawId(embeddedStart.getPid().toString()));
//        workflowFromUnupdateEmbeddedStart.setTargetElementId(getElementRawId(flow.getTarget().getPid().toString()));
//        workflowFromUnupdateEmbeddedStart.setCaseId(getCurrentCaseId());
//        workflowFromUnupdateEmbeddedStart.setCondition(flow.getCondition());
//        workflowFromUnupdateEmbeddedStart.setStartTimeStamp(new Date());
//        persistArrow.add(workflowFromUnupdateEmbeddedStart);
//
//        return persistArrow;
//      }
//    }
//    return null;
//  }
//  
//  public List<WorkflowProgress> findByInprogessArrowIdAndCaseId(String elementId, Long caseId) {
//    List<WorkflowProgress> results = new ArrayList<WorkflowProgress>();
//    int count = 0;
//    int querySize;
//    do {
//      List<WorkflowProgress> currentResult = createSearchQuery().textField("arrowId").isEqualToIgnoringCase(elementId)
//          .and().numberField("caseId").isEqualTo(caseId).and().booleanField("fromInProgessAlternativeOrigin").isTrue()
//          .limit(count, DEFAULT_SEARCH_LIMIT).execute().getAll();
//      results.addAll(currentResult);
//      querySize = currentResult.size();
//      count += querySize;
//    } while (querySize == DEFAULT_SEARCH_LIMIT);
//    return results;
//  }

}
