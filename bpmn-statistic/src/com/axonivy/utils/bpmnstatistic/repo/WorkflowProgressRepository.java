package com.axonivy.utils.bpmnstatistic.repo;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;

import ch.ivyteam.ivy.business.data.store.search.Query;
import ch.ivyteam.ivy.environment.Ivy;

public class WorkflowProgressRepository {
  private static int DEFAULT_SEARCH_LIMIT = 5000;
  private static final WorkflowProgressRepository instance = new WorkflowProgressRepository();
  private static final String PROCESS_RAW_PID_ATTR_NAME = "processRawPid";
  private static final String ARROW_ID_ATTR_NAME = "arrowId";
  private static final String CASE_ID_ATTR_NAME = "caseId";
  private static final String TARGET_ELEMENT_ID_ATTR_NAME = "targetElementId";
  private static final String ORIGIN_ELEMENT_ID_ATTR_NAME = "originElementId";
  private static final String DURATION_UPDATED_ATTR_NAME = "durationUpdated";

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
    progresses.forEach(progress -> save(progress));
  }

  public List<WorkflowProgress> findByProcessRawPid(String id) {
    List<WorkflowProgress> results = new ArrayList<WorkflowProgress>();
    int count = 0;
    int querySize;
    do {
      List<WorkflowProgress> currentResult = createSearchQuery().textField(PROCESS_RAW_PID_ATTR_NAME)
          .containsAllWords(id)
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
      List<WorkflowProgress> currentResult = createSearchQuery().textField(TARGET_ELEMENT_ID_ATTR_NAME)
          .isEqualToIgnoringCase(elementId).and().numberField(CASE_ID_ATTR_NAME).isEqualTo(caseId)
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
      List<WorkflowProgress> currentResult = createSearchQuery().textField(ORIGIN_ELEMENT_ID_ATTR_NAME)
          .isEqualToIgnoringCase(elementId).and().numberField(CASE_ID_ATTR_NAME).isEqualTo(caseId).and()
          .booleanField(DURATION_UPDATED_ATTR_NAME).isFalse().limit(count, DEFAULT_SEARCH_LIMIT).execute().getAll();
      results.addAll(currentResult);
      querySize = currentResult.size();
      count += querySize;
    } while (querySize == DEFAULT_SEARCH_LIMIT);
    return results;
  }

  public List<WorkflowProgress> findByInprogessTargetIdAndCaseId(String targetId, Long caseId) {
    List<WorkflowProgress> results = new ArrayList<WorkflowProgress>();
    int count = 0;
    int querySize;
    do {
      List<WorkflowProgress> currentResult = createSearchQuery().textField(TARGET_ELEMENT_ID_ATTR_NAME)
          .isEqualToIgnoringCase(targetId)
          .and().numberField(CASE_ID_ATTR_NAME).isEqualTo(caseId).and().booleanField(DURATION_UPDATED_ATTR_NAME)
          .isFalse().limit(count, DEFAULT_SEARCH_LIMIT).execute().getAll();
      results.addAll(currentResult);
      querySize = currentResult.size();
      count += querySize;
    } while (querySize == DEFAULT_SEARCH_LIMIT);
    return results;
  }

  public List<WorkflowProgress> findByInprogessArrowIdAndCaseId(String elementId, Long caseId) {
    List<WorkflowProgress> results = new ArrayList<WorkflowProgress>();
    int count = 0;
    int querySize;
    do {
      List<WorkflowProgress> currentResult = createSearchQuery().textField(ARROW_ID_ATTR_NAME)
          .isEqualToIgnoringCase(elementId)
          .and().numberField(CASE_ID_ATTR_NAME).isEqualTo(caseId).and().booleanField(DURATION_UPDATED_ATTR_NAME)
          .isFalse().limit(count, DEFAULT_SEARCH_LIMIT).execute().getAll();
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