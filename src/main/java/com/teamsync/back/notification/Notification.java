package com.teamsync.back.notification;

import com.teamsync.back.channel.Channel;
import com.teamsync.back.channel.message.Message;
import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.task.Task;
import com.teamsync.back.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-108(알림 트리거, US-04): 담당자 지정/상태 변경/마감 임박(D-1, D-0) 발생 시 생성되는
 * 수신자 개인화 알림. createdAt(BaseTimeEntity)이 곧 알림 발생 시각이다.
 * task는 알림 클릭 시 해당 태스크로 이동하는 딥링크 참조용으로만 쓰이며, 태스크가 나중에
 * 삭제되어도 알림 이력 자체는 남아야 하므로 nullable(ON DELETE SET NULL)로 구성한다.
 */
@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationType type;

	@Column(nullable = false, length = 500)
	private String message;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id")
	private Task task;

	// FR-105-A/FR-202-A(멘션 딥링크): 메시지 멘션 알림은 채널/메시지로 이동하는 딥링크 참조를 함께 남긴다.
	// task와 마찬가지로 원본이 나중에 삭제되어도 알림 이력은 보존해야 하므로 nullable(ON DELETE SET NULL)이다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "channel_id")
	private Channel channel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id")
	private Message linkedMessage;

	@Column(name = "is_read", nullable = false)
	private boolean read;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	public Notification(User recipient, NotificationType type, String message, Task task) {
		this.recipient = recipient;
		this.type = type;
		this.message = message;
		this.task = task;
		this.read = false;
	}

	/**
	 * FR-105-A: 태스크 댓글 멘션 알림. task 딥링크만 채우며 channel/message 딥링크는 두지 않는다.
	 */
	public static Notification forTaskMention(User recipient, String message, Task task) {
		return new Notification(recipient, NotificationType.MENTION, message, task);
	}

	/**
	 * FR-202-A: 메시지 멘션 알림. channel_id + message_id 딥링크를 함께 채워 프론트가 해당 메시지로
	 * 이동할 수 있게 한다(task 딥링크는 없음).
	 */
	public static Notification forMessageMention(User recipient, String message, Channel channel,
			Message linkedMessage) {
		Notification notification = new Notification(recipient, NotificationType.MENTION, message, null);
		notification.channel = channel;
		notification.linkedMessage = linkedMessage;
		return notification;
	}

	public void markAsRead() {
		if (this.read) {
			return;
		}
		this.read = true;
		this.readAt = LocalDateTime.now();
	}
}
