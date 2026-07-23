package com.teamsync.back.notification;

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

	public void markAsRead() {
		if (this.read) {
			return;
		}
		this.read = true;
		this.readAt = LocalDateTime.now();
	}
}
