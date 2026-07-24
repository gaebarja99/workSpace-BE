package com.teamsync.back.task.dto;

import java.util.List;

/**
 * FR-107: GET /api/tasks/{taskId}/dependencies 응답.
 */
public record TaskDependencyListResponse(
		List<TaskDependencyItemResponse> predecessors,
		List<TaskDependencyItemResponse> successors
) {
}
