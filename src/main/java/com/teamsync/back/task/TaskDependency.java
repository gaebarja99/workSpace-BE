package com.teamsync.back.task;

import com.teamsync.back.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * FR-107(P2, 시각화 없이 데이터 모델+API만 유지되는 축소 범위): 태스크 간 선후행 의존관계.
 * predecessorTask(선행)가 끝나야 successorTask(후행)가 진행된다는 "관계" 자체만 표현하며,
 * 이 관계로 인해 태스크 상태 전환을 막는 등의 비즈니스 규칙은 이번 범위에 포함되지 않는다
 * (간트뷰/타임라인 시각화가 Phase 2 이후로 밀리면서 순환 감지·CRUD API까지만 유지).
 * predecessor/successor 한 쌍은 유일해야 하며(V17 UNIQUE 제약), 자기 자신을 가리킬 수 없다(CHECK 제약,
 * 애플리케이션 레벨에서는 TaskDependencyService가 SelfDependencyException으로 선제 검증한다).
 * 태스크 삭제 시 관련 행은 ON DELETE CASCADE로 자동 정리된다(FR-001 삭제 정책 변경 없음).
 */
@Getter
@Entity
@Table(name = "task_dependencies")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskDependency {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "predecessor_task_id", nullable = false)
	private Task predecessorTask;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "successor_task_id", nullable = false)
	private Task successorTask;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public TaskDependency(Task predecessorTask, Task successorTask, User createdBy) {
		this.predecessorTask = predecessorTask;
		this.successorTask = successorTask;
		this.createdBy = createdBy;
	}
}
