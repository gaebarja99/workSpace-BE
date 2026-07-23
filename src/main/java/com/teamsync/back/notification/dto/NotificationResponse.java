package com.teamsync.back.notification.dto;

import com.teamsync.back.notification.Notification;
import com.teamsync.back.notification.NotificationType;
import com.teamsync.back.task.Task;
import java.time.LocalDateTime;

/**
 * FR-108 알림 조회/읽음 처리 공통 응답. task가 null(삭제되었거나 애초에 태스크와 무관한 알림)이면
 * taskId/projectId도 함께 null로 내려 프론트엔드가 딥링크 버튼을 조건부로 숨길 수 있게 한다.
 */
public record NotificationResponse(
		Long id,
		NotificationType type,
		String message,
		Long taskId,
		Long projectId,
		boolean read,
		LocalDateTime createdAt
) {
	public static NotificationResponse from(Notification notification) {
		Task task = notification.getTask();
		Long taskId = task != null ? task.getId() : null;
		Long projectId = task != null ? task.getProject().getId() : null;
		return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				notification.getMessage(),
				taskId,
				projectId,
				notification.isRead(),
				notification.getCreatedAt());
	}
}
