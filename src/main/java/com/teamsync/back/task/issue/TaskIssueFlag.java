package com.teamsync.back.task.issue;

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
 * FR-406(이슈/리스크 자동 플래그, 완전판): FR-401 주간보고 생성 시점의 일회성 OVERDUE/STALE
 * 스냅샷과 달리, TaskIssueFlagBatchService의 일 배치가 매일 재계산해 영속적으로 관리하는
 * 플래그 1건. 동일 태스크+kind 조합의 OPEN 행은 V18 부분 유니크 인덱스로 최대 1건만 허용되고,
 * RESOLVED 이력은 (자동 해소 시마다, 또는 수동 resolve 시마다) 여러 건 쌓일 수 있다.
 * task는 ON DELETE CASCADE(태스크 삭제 시 플래그도 함께 삭제), resolvedBy는 ON DELETE SET NULL
 * (해결한 사용자가 나중에 삭제돼도 플래그 이력 자체는 남는다)이다.
 */
@Getter
@Entity
@Table(name = "task_issue_flags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskIssueFlag extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskIssueKind kind;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskIssueStatus status;

	@Column(nullable = false, length = 500)
	private String detail;

	@Column(name = "detected_at", nullable = false)
	private LocalDateTime detectedAt;

	@Column(name = "resolved_at")
	private LocalDateTime resolvedAt;

	// 수동 해결(PATCH /resolve) 시에는 요청자, 배치가 조건 해소를 감지해 자동 RESOLVED 처리할 때는
	// null로 남겨 "시스템이 처리했다"는 것을 구분한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resolved_by_id")
	private User resolvedBy;

	public TaskIssueFlag(Task task, TaskIssueKind kind, String detail, LocalDateTime detectedAt) {
		this.task = task;
		this.kind = kind;
		this.detail = detail;
		this.detectedAt = detectedAt;
		this.status = TaskIssueStatus.OPEN;
	}

	/**
	 * 이미 RESOLVED면 아무 것도 하지 않는다(호출부가 멱등적으로 여러 번 호출해도 안전 -
	 * PATCH .../resolve 계약: "이미 RESOLVED면 그대로 현재 상태 반환, 에러 아님").
	 * resolvedBy가 null이면 배치가 조건 해소를 감지해 자동으로 처리한 것이다.
	 */
	public void resolve(User resolvedBy) {
		if (this.status == TaskIssueStatus.RESOLVED) {
			return;
		}
		this.status = TaskIssueStatus.RESOLVED;
		this.resolvedAt = LocalDateTime.now();
		this.resolvedBy = resolvedBy;
	}
}
