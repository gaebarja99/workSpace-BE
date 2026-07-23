package com.teamsync.back.task;

import com.teamsync.back.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * FR-105-B(태스크 활동 로그): 태스크 생성/상태 변경/담당자 변경/댓글 작성 등 정형 이벤트를 시간순으로 기록한다.
 * 태스크 변경 트랜잭션과 동일 트랜잭션 안에서 append 되며(TaskService/TaskActivityService), actor가 null이면
 * 시스템/자동 발생 활동으로 간주한다. createdAt이 곧 활동 발생 시각이다(TaskComment와 동일하게 생성 시각만 관리).
 */
@Getter
@Entity
@Table(name = "task_activities")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskActivity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private User actor;

	@Enumerated(EnumType.STRING)
	@Column(name = "activity_type", nullable = false, length = 30)
	private TaskActivityType activityType;

	@Column(columnDefinition = "TEXT")
	private String detail;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public TaskActivity(Task task, User actor, TaskActivityType activityType, String detail) {
		this.task = task;
		this.actor = actor;
		this.activityType = activityType;
		this.detail = detail;
	}
}
