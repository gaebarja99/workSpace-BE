package com.teamsync.back.notification.dto;

import com.teamsync.back.channel.Channel;
import com.teamsync.back.channel.message.Message;
import com.teamsync.back.notification.Notification;
import com.teamsync.back.notification.NotificationType;
import com.teamsync.back.task.Task;
import java.time.LocalDateTime;

/**
 * FR-108 알림 조회/읽음 처리 공통 응답. task가 null(삭제되었거나 애초에 태스크와 무관한 알림)이면
 * taskId/projectId도 함께 null로 내려 프론트엔드가 딥링크 버튼을 조건부로 숨길 수 있게 한다.
 * FR-202-A(메시지 멘션): task가 없는 대신 channelId/messageId 딥링크가 채워지며, 이 경우 projectId는
 * channel의 project에서 파생한다.
 */
public record NotificationResponse(
		Long id,
		NotificationType type,
		String message,
		Long taskId,
		Long projectId,
		Long channelId,
		Long messageId,
		boolean read,
		LocalDateTime createdAt
) {
	public static NotificationResponse from(Notification notification) {
		Task task = notification.getTask();
		Channel channel = notification.getChannel();
		Message linkedMessage = notification.getLinkedMessage();

		Long taskId = task != null ? task.getId() : null;
		Long channelId = channel != null ? channel.getId() : null;
		Long messageId = linkedMessage != null ? linkedMessage.getId() : null;
		Long projectId = task != null
				? task.getProject().getId()
				: (channel != null ? channel.getProject().getId() : null);

		return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				notification.getMessage(),
				taskId,
				projectId,
				channelId,
				messageId,
				notification.isRead(),
				notification.getCreatedAt());
	}
}
