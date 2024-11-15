package com.axonivy.utils.bpmnstatistic.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.bpmnstatistic.bo.TaskOccurrence;
import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.constants.ProcessAnalyticsConstants;
import com.axonivy.utils.bpmnstatistic.enums.IvyVariable;
import com.axonivy.utils.bpmnstatistic.internal.ProcessUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.custom.field.ICustomField;
import ch.ivyteam.ivy.workflow.custom.field.ICustomFieldMeta;
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

  public static Map<ICustomFieldMeta, List<Object>> getCaseAndTaskCustomFields(String selectedPid,
      TimeIntervalFilter timeIntervalFilter, Map<ICustomFieldMeta, List<Object>> customFieldsByType) {
    return Sudo.get(() -> {
      TaskQuery taskQuery = TaskQuery.create().where().requestPath().isLike(getRequestPath(selectedPid)).and()
          .startTimestamp().isGreaterOrEqualThan(timeIntervalFilter.getFrom()).and().startTimestamp()
          .isLowerOrEqualThan(timeIntervalFilter.getTo());

      List<ITask> tasks = new ArrayList<>();
      int maxQueryResults = Integer.valueOf(Ivy.var().get(IvyVariable.MAX_QUERY_RESULTS.getVariableName()));
      Integer startIndex = 0;

      do {
        tasks = Ivy.wf().getTaskQueryExecutor().getResults(taskQuery, startIndex, maxQueryResults);
        startIndex += maxQueryResults;
      } while (maxQueryResults == tasks.size());

      List<ICustomField<?>> allCustomFields = getAllCustomFields(tasks);
      customFieldsByType.clear();

      for (ICustomField<?> customField : allCustomFields) {
        ICustomFieldMeta fieldMeta = customField.meta();
        Object customFieldValue = customField.getOrNull();

        if (customFieldValue != null) {
          List<Object> addedCustomFieldValues = customFieldsByType.computeIfAbsent(fieldMeta, k -> new ArrayList<>());

          if (!addedCustomFieldValues.contains(customFieldValue)) {
            addedCustomFieldValues.add(customFieldValue);
          }
        }
      }
      return customFieldsByType;
    });
  }

  private static List<ICustomField<?>> getAllCustomFields(List<ITask> tasks) {
    List<ICustomField<?>> allCustomFields = new ArrayList<>();
    for (ITask task : tasks) {
      allCustomFields.addAll(task.customFields().all());
      allCustomFields.addAll(task.getCase().getBusinessCase().customFields().all());
      allCustomFields.addAll(task.getCase().customFields().all());
    }
    return allCustomFields;
  }

  private static String getRequestPath(String processId) {
    return String.format(ProcessAnalyticsConstants.LIKE_TEXT_SEARCH, processId);
  }
}
