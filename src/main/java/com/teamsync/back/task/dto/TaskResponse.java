package com.teamsync.back.task.dto;

import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskMessageLink;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 단건 상세 조회(GET /api/tasks/{taskId}) 응답. 체크리스트를 포함한다.
 * FR-301/303(US-09): linkedChannelId/linkedMessageId는 이 태스크가 메시지 변환(FR-301)으로 생성된
 * 경우에만 값이 채워지며(TaskMessageLink 존재), 프론트는 이 값으로 "관련 대화 보기"(FR-303) 버튼
 * 노출 여부를 판단한다(별도 조회 엔드포인트 없음). channelNotificationsEnabled는 FR-302 on/off 토글.
 */
public record TaskResponse(
		Long id,
		Long projectId,
		String title,
		String description,
		TaskStatus status,
		TaskPriority priority,
		LocalDate startDate,
		LocalDate dueDate,
		List<AssigneeResponse> assignees,
		List<ChecklistItemResponse> checklistItems,
		Long linkedChannelId,
		Long linkedMessageId,
		boolean channelNotificationsEnabled,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static TaskResponse from(Task task, TaskMessageLink link) {
		return new TaskResponse(
				task.getId(),
				task.getProject().getId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getPriority(),
				task.getStartDate(),
				task.getDueDate(),
				task.getAssignees().stream().map(AssigneeResponse::from).toList(),
				task.getChecklistItems().stream().map(ChecklistItemResponse::from).toList(),
				link != null ? link.getMessage().getChannel().getId() : null,
				link != null ? link.getMessage().getId() : null,
				task.isChannelNotificationsEnabled(),
				task.getCreatedAt(),
				task.getUpdatedAt());
	}
}
