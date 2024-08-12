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

  public List<WorkflowProgress> findByProcessRawPid(String id) {
    return executeAll(filterByProcessRawPid(id));
  }

  public List<WorkflowProgress> findByProcessRawPidInTime(String id, TimeIntervalFilter timeIntervalFilter) {
    Filter<WorkflowProgress> filter = filterByProcessRawPid(id).and().dateTimeField("createdAt").isInDateRange(timeIntervalFilter.getFrom(), timeIntervalFilter.getTo());
    return executeAll(filter);
  }

  private Filter<WorkflowProgress> filterByProcessRawPid(String id) {
    Filter<WorkflowProgress> filter = createSearchQuery().textField("processRawPid").containsAllWords(id);
    return filter;
  }

  public List<WorkflowProgress> findByTargetElementIdAndCaseId(String elementId, Long caseId) {
    Filter<WorkflowProgress> filter = createSearchQuery().textField("targetElementId").isEqualToIgnoringCase(elementId)
        .and().numberField("caseId").isEqualTo(caseId);
    return executeAll(filter);
  }

  private List<WorkflowProgress> executeAll(Filter<WorkflowProgress> filter) {
    if (filter == null) {
      return List.of();
    }

    List<WorkflowProgress> results = new ArrayList<>();
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

  private Query<WorkflowProgress> createSearchQuery() {
    return Ivy.repo().search(getType());
  }
}