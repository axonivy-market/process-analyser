package com.axonivy.solutions.process.analyser.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.bo.CustomFieldFilter;
import com.axonivy.solutions.process.analyser.bo.TaskOccurrence;
import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;
import com.axonivy.solutions.process.analyser.enums.IvyVariable;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.custom.field.ICustomField;
import ch.ivyteam.ivy.workflow.query.TaskQuery;

public class IvyTaskOccurrenceService {

  private IvyTaskOccurrenceService() {
  }

  public static HashMap<String, Integer> countTaskOccurrencesByProcessId(String processId) {
    HashMap<String, TaskOccurrence> taskOccurrenceMap = getHashMapTaskOccurrencesByProcessId(processId);
    return correctTaskOccurrences(taskOccurrenceMap);
  }

  private static HashMap<String, TaskOccurrence> getHashMapTaskOccurrencesByProcessId(String processId) {
    return Sudo.get(() -> {
      TaskQuery taskQuery = TaskQuery.create().where().requestPath()
          .isLike(getRequestPath(processId)).orderBy()
          .startTaskSwitchEventId();
      HashMap<String, TaskOccurrence> map = new HashMap<>();
      countTaskOccurrencesByTaskQuery(map, taskQuery);
      return map;
    });
  }

  private static void countTaskOccurrencesByTaskQuery(HashMap<String, TaskOccurrence> map, TaskQuery taskQuery) {
    List<ITask> tasks = new ArrayList<>();
    int maxQueryResults = Integer.valueOf(Ivy.var().get(IvyVariable.MAX_QUERY_RESULTS.getVariableName()));
    Integer startIndex = 0;
    do {
      tasks = Ivy.wf().getTaskQueryExecutor().getResults(taskQuery, startIndex, maxQueryResults);
      countTaskOccurrences(map, tasks);
      startIndex += maxQueryResults;
    } while (maxQueryResults == tasks.size());
  }

  private static void countTaskOccurrences(HashMap<String, TaskOccurrence> taskOccurrenceMap, List<ITask> tasks) {
    for (ITask iTask : tasks) {
      String taskElementId = ProcessUtils.getTaskElementIdFromRequestPath(iTask.getRequestPath());
      updateTaskOccurrencesMap(taskOccurrenceMap, taskElementId, iTask.getStartSwitchEvent().getId());
    }
  }

  private static void updateTaskOccurrencesMap(HashMap<String, TaskOccurrence> taskOccurrenceMap, String taskElementId,
      Long startTaskSwitchEventId) {
    if (StringUtils.isNotBlank(taskElementId)) {
      TaskOccurrence taskOccurrence = getCountedTaskOccurrence(taskOccurrenceMap, taskElementId,
          startTaskSwitchEventId);
      taskOccurrenceMap.put(taskElementId, taskOccurrence);
    }
  }

  private static TaskOccurrence getCountedTaskOccurrence(HashMap<String, TaskOccurrence> taskOccurrenceMap,
      String taskElementId, Long startTaskSwitchEventId) {
    TaskOccurrence taskOccurrence = taskOccurrenceMap.get(taskElementId);
    if (taskOccurrence != null) {
      if (startTaskSwitchEventId != null && !taskOccurrence.getStartSwitchEventId().equals(startTaskSwitchEventId)) {
        taskOccurrence.setOccurrence(taskOccurrence.getOccurrence() + 1);
        taskOccurrence.setStartSwitchEventId(startTaskSwitchEventId);
      }
    } else {
      taskOccurrence = new TaskOccurrence(startTaskSwitchEventId, 1);
    }
    return taskOccurrence;
  }

  private static HashMap<String, Integer> correctTaskOccurrences(HashMap<String, TaskOccurrence> taskOccurrenceMap) {
    HashMap<String, Integer> result = new HashMap<>();
    for (Map.Entry<String, TaskOccurrence> entry : taskOccurrenceMap.entrySet()) {
      result.put(entry.getKey(), entry.getValue().getOccurrence());
    }
    return result;
  }

  /**
   * Get all custom fields from cases/ business cases and tasks
   */
  public static List<CustomFieldFilter> getCaseAndTaskCustomFields(String selectedPid,
      TimeIntervalFilter timeIntervalFilter) {
    List<CustomFieldFilter> customFieldsByType = new ArrayList<>();

    return Sudo.get(() -> {
      TaskQuery taskQuery = TaskQuery.create().where().requestPath().isLike(getRequestPath(selectedPid)).and()
          .startTimestamp().isGreaterOrEqualThan(timeIntervalFilter.getFrom()).and().startTimestamp()
          .isLowerOrEqualThan(timeIntervalFilter.getTo());

      List<ITask> tasks = new ArrayList<>();
      int maxQueryResults = Integer.valueOf(Ivy.var().get(IvyVariable.MAX_QUERY_RESULTS.getVariableName()));
      int startIndex = 0;

      do {
        tasks = Ivy.wf().getTaskQueryExecutor().getResults(taskQuery, startIndex, maxQueryResults);
        startIndex += maxQueryResults;
      } while (maxQueryResults == tasks.size());
      return getCaseAndTaskCustomFields(tasks, customFieldsByType);
    });
  }

  /**
   * Retrieves all custom fields from the provided list of tasks and their associated cases/business cases,
   * and adds them to the given list of custom field filters.
   *
   * @param tasks the list of tasks from which to extract custom fields
   * @param customFieldsByType the list to which found custom field filters will be added
   * @return the updated list of custom field filters containing all found custom fields
   */
  public static List<CustomFieldFilter> getCaseAndTaskCustomFields(List<ITask> tasks, List<CustomFieldFilter> customFieldsByType) {
    if (CollectionUtils.isNotEmpty(tasks)) {
      for (ITask task : tasks) {
        List<ICustomField<?>> allCustomFieldsFromCases = new ArrayList<>();
        allCustomFieldsFromCases.addAll(task.getCase().getBusinessCase().customFields().all());
        allCustomFieldsFromCases.addAll(task.getCase().customFields().all());
        addCustomFieldsToCustomFieldsByType(task.customFields().all(), false, customFieldsByType);
        addCustomFieldsToCustomFieldsByType(allCustomFieldsFromCases, true, customFieldsByType);
      }
    }
    return customFieldsByType;
  }

  private static void addCustomFieldsToCustomFieldsByType(List<ICustomField<?>> customFields,
      boolean isCustomFieldFromCase, List<CustomFieldFilter> customFieldsByType) {
    for (ICustomField<?> customField : customFields) {
      Object customFieldValue = customField.getOrNull();
      if (customFieldValue != null) {
        CustomFieldFilter customFieldFilter = new CustomFieldFilter();
        customFieldFilter.setCustomFieldMeta(customField.meta());
        customFieldFilter.setCustomFieldFromCase(isCustomFieldFromCase);

        CustomFieldFilter existingCustomFieldFilter =
            customFieldsByType.stream().filter(filter -> filter.equals(customFieldFilter)).findAny().orElse(null);

        if (existingCustomFieldFilter != null) {
          if (!existingCustomFieldFilter.getAvailableCustomFieldValues().contains(customFieldValue)) {
            existingCustomFieldFilter.getAvailableCustomFieldValues().add(customFieldValue);
          }
        } else {
          customFieldFilter.setCustomFieldValues(new ArrayList<>());
          customFieldFilter.setAvailableCustomFieldValues(new ArrayList<>());
          customFieldFilter.getAvailableCustomFieldValues().add(customFieldValue);
          customFieldsByType.add(customFieldFilter);
        }
      }
    }
  }

  private static String getRequestPath(String processId) {
    return String.format(ProcessAnalyticsConstants.LIKE_TEXT_SEARCH, processId);
  }
}