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
		Ivy.repo().save(progress).getId();
	}

	public List<WorkflowProgress> findByProcessRawPid(String id) {
		return createSearchQuery().textField("processRawPid").containsAllWords(id).execute().getAll();
	}

	public List<WorkflowProgress> findByTargetElementIdAndCaseId(String elementId, Long caseId) {
		List<WorkflowProgress> result = new ArrayList<WorkflowProgress>();
		int queryListSize = 0;
//		Thread.sleep(2000);
		do {
			List<WorkflowProgress> queryList = createSearchQuery().textField("targetElementId")
					.isEqualToIgnoringCase(elementId).and().numberField("caseId").isEqualTo(caseId)
					.limit(result.size(), DEFAULT_SEARCH_LIMIT).execute().getAll();
			queryListSize = queryList.size();
			result.addAll(queryList);
		} while (queryListSize != 0 && queryListSize % DEFAULT_SEARCH_LIMIT == 0);
		return result;
	}

	private Query<WorkflowProgress> createSearchQuery() {
		return Ivy.repo().search(getType());
	}
}