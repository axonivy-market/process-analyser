package com.axonivy.utils.bpmnstatistic.repo;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;

import ch.ivyteam.ivy.business.data.store.search.Query;
import ch.ivyteam.ivy.environment.Ivy;

public class WorkflowProgressRepository {
  private static int DEFAULT_SEARCH_LIMIT = 5000;
  private static final WorkflowProgressRepository instance = new WorkflowProgressRepository();

  private WorkflowProgressRepository() {
  }

  public static WorkflowProgressRepository getInstance() {
    return instance;
  }

  public Class<WorkflowProgress> getType() {
    return WorkflowProgress.class;
  }

  public void save(WorkflowProgress progress) {
    Ivy.repo().save(progress);
  }

  public void delete(WorkflowProgress progress) {
    Ivy.repo().delete(progress);
  }

  public void save(List<WorkflowProgress> progresses) {
    progresses.stream().forEach(progress -> save(progress));
  }

  public List<WorkflowProgress> findByProcessRawPid(String id) {
    List<WorkflowProgress> results = new ArrayList<WorkflowProgress>();
    int count = 0;
    int querySize;
    do {
      List<WorkflowProgress> currentResult = createSearchQuery().textField("processRawPid").containsAllWords(id)
          .limit(count, DEFAULT_SEARCH_LIMIT).execute().getAll();
      results.addAll(currentResult);
      querySize = currentResult.size();
      count += querySize;
    } while (querySize == DEFAULT_SEARCH_LIMIT);
    return results;
  }

  public List<WorkflowProgress> findByTargetElementIdAndCaseId(String elementId, Long caseId) {
    List<WorkflowProgress> results = new ArrayList<WorkflowProgress>();
    int count = 0;
    int querySize;
    do {
      List<WorkflowProgress> currentResult = createSearchQuery().textField("targetElementId")
          .isEqualToIgnoringCase(elementId).and().numberField("caseId").isEqualTo(caseId)
          .limit(count, DEFAULT_SEARCH_LIMIT).execute().getAll();
      results.addAll(currentResult);
      querySize = currentResult.size();
      count += querySize;
    } while (querySize == DEFAULT_SEARCH_LIMIT);
    return results;
  }

  public List<WorkflowProgress> findByInprogressAlternativeIdAndCaseId(String elementId, Long caseId) {
    List<WorkflowProgress> results = new ArrayList<WorkflowProgress>();
    int count = 0;
    int querySize;
    do {
      List<WorkflowProgress> currentResult = createSearchQuery().textField("originElementId")
          .isEqualToIgnoringCase(elementId).and().numberField("caseId").isEqualTo(caseId).and()
          .booleanField("fromInProgessAlternativeOrigin").isTrue().limit(count, DEFAULT_SEARCH_LIMIT).execute()
          .getAll();
      results.addAll(currentResult);
      querySize = currentResult.size();
      count += querySize;
    } while (querySize == DEFAULT_SEARCH_LIMIT);
    return results;
  }

  private Query<WorkflowProgress> createSearchQuery() {
    return Ivy.repo().search(getType());
  }
}