package com.teamsync.back.task.dto;

import com.teamsync.back.task.TaskDependency;

/**
 * FR-107: GET /api/tasks/{taskId}/dependencies 응답의 predecessors/successors 목록 각 항목.
 */
public record TaskDependencyItemResponse(
		Long dependencyId,
		TaskSummaryResponse task
) {
	public static TaskDependencyItemResponse ofPredecessor(TaskDependency dependency) {
		return new TaskDependencyItemResponse(dependency.getId(), TaskSummaryResponse.from(dependency.getPredecessorTask()));
	}

	public static TaskDependencyItemResponse ofSuccessor(TaskDependency dependency) {
		return new TaskDependencyItemResponse(dependency.getId(), TaskSummaryResponse.from(dependency.getSuccessorTask()));
	}
}
