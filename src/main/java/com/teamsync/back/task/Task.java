package com.teamsync.back.task;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.project.Project;
import com.teamsync.back.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-101(보드) / FR-102(태스크 카드) 최소 골격.
 * 라벨/첨부파일/댓글/활동로그(FR-105), 반복(FR-106), 의존관계(FR-107)는 이번 범위 밖이며
 * 후속 마이그레이션/엔티티 확장으로 다룬다.
 * 담당자(assignees)는 프로젝트와 마찬가지로 User를 단방향 ManyToMany로 참조하며,
 * 체크리스트(checklistItems)는 TaskChecklistItemRepository로 직접 관리하되 조회 시
 * 이 컬렉션(OrderBy position)으로도 일관된 순서를 보장한다.
 */
@Getter
@Entity
@Table(name = "tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskPriority priority;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "task_assignees",
			joinColumns = @JoinColumn(name = "task_id"),
			inverseJoinColumns = @JoinColumn(name = "user_id"))
	private Set<User> assignees = new LinkedHashSet<>();

	@OneToMany(mappedBy = "task", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position ASC, id ASC")
	private List<TaskChecklistItem> checklistItems = new ArrayList<>();

	// FR-302(US-09/10): 태스크 생성/상태변경/완료 시 채널에 시스템 메시지를 자동 게시할지 여부(on/off 토글).
	// PATCH /api/tasks/{taskId}로 변경 가능하며, 기본값은 true(항상 알린다)다.
	@Column(name = "channel_notifications_enabled", nullable = false)
	private boolean channelNotificationsEnabled = true;

	public Task(Project project, String title, String description, TaskPriority priority, TaskStatus status,
			LocalDate startDate, LocalDate dueDate, User createdBy, Set<User> assignees) {
		this.project = project;
		this.title = title;
		this.description = description;
		this.priority = priority;
		this.status = status;
		this.startDate = startDate;
		this.dueDate = dueDate;
		this.createdBy = createdBy;
		this.assignees = new LinkedHashSet<>(assignees);
	}

	public void changeTitle(String title) {
		this.title = title;
	}

	public void changeDescription(String description) {
		this.description = description;
	}

	public void changePriority(TaskPriority priority) {
		this.priority = priority;
	}

	public void changeStatus(TaskStatus status) {
		this.status = status;
	}

	public void changeStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public void changeDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public void changeAssignees(Set<User> assignees) {
		this.assignees.clear();
		this.assignees.addAll(assignees);
	}

	public void changeChannelNotificationsEnabled(boolean channelNotificationsEnabled) {
		this.channelNotificationsEnabled = channelNotificationsEnabled;
	}
}
