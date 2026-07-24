package com.teamsync.back.task.dto;

import com.teamsync.back.task.TaskDependency;

/**
 * FR-107: POST /api/tasks/{taskId}/dependencies 응답(201).
 */
public record TaskDependencyResponse(
		Long dependencyId,
		TaskSummaryResponse predecessorTask,
		TaskSummaryResponse successorTask
) {
	public static TaskDependencyResponse from(TaskDependency dependency) {
		return new TaskDependencyResponse(
				dependency.getId(),
				TaskSummaryResponse.from(dependency.getPredecessorTask()),
				TaskSummaryResponse.from(dependency.getSuccessorTask()));
	}
}
