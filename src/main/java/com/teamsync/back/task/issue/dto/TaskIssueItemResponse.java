package com.teamsync.back.task.issue.dto;

import com.teamsync.back.task.issue.TaskIssueFlag;
import com.teamsync.back.task.issue.TaskIssueKind;
import com.teamsync.back.task.issue.TaskIssueStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FR-406 이슈 플래그 1건 응답. GET /api/projects/{projectId}/issues의 issues 배열 원소이자
 * PATCH .../issues/{issueId}/resolve의 단건 응답이기도 하다.
 */
public record TaskIssueItemResponse(
		Long id,
		Long taskId,
		String taskTitle,
		TaskIssueKind kind,
		TaskIssueStatus status,
		String detail,
		List<TaskIssueAssigneeView> assignees,
		LocalDateTime detectedAt,
		LocalDateTime resolvedAt,
		String resolvedBy) {

	public static TaskIssueItemResponse from(TaskIssueFlag flag) {
		List<TaskIssueAssigneeView> assignees = flag.getTask().getAssignees().stream()
				.map(TaskIssueAssigneeView::from)
				.toList();
		return new TaskIssueItemResponse(
				flag.getId(),
				flag.getTask().getId(),
				flag.getTask().getTitle(),
				flag.getKind(),
				flag.getStatus(),
				flag.getDetail(),
				assignees,
				flag.getDetectedAt(),
				flag.getResolvedAt(),
				flag.getResolvedBy() != null ? flag.getResolvedBy().getName() : null);
	}
}
