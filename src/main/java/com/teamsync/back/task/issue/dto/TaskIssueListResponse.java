package com.teamsync.back.task.issue.dto;

import java.util.List;

/** FR-406: GET /api/projects/{projectId}/issues 응답. */
public record TaskIssueListResponse(List<TaskIssueItemResponse> issues) {
}
