package com.axonivy.utils.bpmnstatistic.repo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.bo.WorkflowProgress;

import ch.ivyteam.ivy.business.data.store.search.Filter;
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
  private static final String CREATED_AT_ATTR_NAME = "createdAt";

  private WorkflowProgressRepository() {
  }

  public static WorkflowProgressRepository getInstance() {
    return instance;
  }

  public Class<WorkflowProgress> getType() {
    return WorkflowProgress.class;
  }

  public void save(WorkflowProgress progress) {
    if (progress != null && progress.getCreatedAt() == null) {
      progress.setCreatedAt(new Date());
    }
    Ivy.repo().save(progress);
  }

  public void delete(WorkflowProgress progress) {
    Ivy.repo().delete(progress);
  }

  public void save(List<WorkflowProgress> progresses) {
    progresses.forEach(progress -> save(progress));
  }

  public List<WorkflowProgress> findByProcessRawPid(String id) {
    return executeAll(filterByProcessRawPid(id));
  }

  public List<WorkflowProgress> findByProcessRawPidInTime(String id, TimeIntervalFilter timeIntervalFilter) {
    Filter<WorkflowProgress> filter = filterByProcessRawPid(id).and().dateTimeField(CREATED_AT_ATTR_NAME)
        .isInDateRange(timeIntervalFilter.getFrom(), timeIntervalFilter.getTo());
    return executeAll(filter);
  }

  private Filter<WorkflowProgress> filterByProcessRawPid(String id) {
    Filter<WorkflowProgress> filter = createSearchQuery().textField(PROCESS_RAW_PID_ATTR_NAME).containsAllWords(id);
    return filter;
  }

  public List<WorkflowProgress> findByTargetElementIdAndCaseId(String elementId, Long caseId) {
    Filter<WorkflowProgress> filter = createSearchQuery().textField(TARGET_ELEMENT_ID_ATTR_NAME)
        .isEqualToIgnoringCase(elementId).and().numberField(CASE_ID_ATTR_NAME).isEqualTo(caseId);
    return executeAll(filter);
  }

  private List<WorkflowProgress> executeAll(Filter<WorkflowProgress> filter) {
    List<WorkflowProgress> results = new ArrayList<>();
    if (filter == null) {
      return results;
    }

    int count = 0;
    int querySize;
    do {
      var currentResult = filter.limit(count, DEFAULT_SEARCH_LIMIT).execute().getAll();
      results.addAll(currentResult);
      querySize = currentResult.size();
      count += querySize;
    } while (querySize == DEFAULT_SEARCH_LIMIT);
    return results;
  }

  public List<WorkflowProgress> findByInprogressAlternativeIdAndCaseId(String elementId, Long caseId) {
    Filter<WorkflowProgress> filter = createSearchQuery().textField(ORIGIN_ELEMENT_ID_ATTR_NAME)
        .isEqualToIgnoringCase(elementId).and().numberField(CASE_ID_ATTR_NAME).isEqualTo(caseId).and()
        .booleanField(DURATION_UPDATED_ATTR_NAME).isFalse();
    return executeAll(filter);
  }

  public List<WorkflowProgress> findByInprogessTargetIdAndCaseId(String targetId, Long caseId) {
    Filter<WorkflowProgress> filter = createSearchQuery().textField(TARGET_ELEMENT_ID_ATTR_NAME)
        .isEqualToIgnoringCase(targetId).and().numberField(CASE_ID_ATTR_NAME).isEqualTo(caseId).and()
        .booleanField(DURATION_UPDATED_ATTR_NAME).isFalse();
    return executeAll(filter);
  }

  public List<WorkflowProgress> findByInprogessArrowIdAndCaseId(String elementId, Long caseId) {
    Filter<WorkflowProgress> filter = createSearchQuery().textField(ARROW_ID_ATTR_NAME).isEqualToIgnoringCase(elementId)
        .and().numberField(CASE_ID_ATTR_NAME).isEqualTo(caseId).and().booleanField(DURATION_UPDATED_ATTR_NAME)
        .isFalse();
    return executeAll(filter);
  }

  private Query<WorkflowProgress> createSearchQuery() {
    return Ivy.repo().search(getType());
  }
}