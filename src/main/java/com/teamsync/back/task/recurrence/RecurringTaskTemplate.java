package com.teamsync.back.task.recurrence;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.project.Project;
import com.teamsync.back.task.TaskPriority;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-106(반복 태스크 템플릿, 주간/월간): 이 템플릿 자체는 태스크가 아니라 "생성 규칙"만 보관하며,
 * 실제 Task는 RecurringTaskSchedulerService의 일 배치(@Scheduled)가 이 템플릿을 읽어 매번 새로
 * 생성한다. recurrenceType=WEEKLY면 dayOfWeek만, MONTHLY면 dayOfMonth만 값을 가진다(상호배타,
 * RecurringTaskTemplateService에서 검증). lastGeneratedAt은 배치의 중복 생성 방지용 커서다.
 */
@Getter
@Entity
@Table(name = "recurring_task_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringTaskTemplate extends BaseTimeEntity {

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
	private TaskPriority priority;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "recurring_task_template_assignees",
			joinColumns = @JoinColumn(name = "template_id"),
			inverseJoinColumns = @JoinColumn(name = "user_id"))
	private Set<User> assignees = new LinkedHashSet<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "recurrence_type", nullable = false, length = 20)
	private RecurrenceType recurrenceType;

	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", length = 20)
	private DayOfWeek dayOfWeek;

	@Column(name = "day_of_month")
	private Integer dayOfMonth;

	@Column(name = "due_in_days", nullable = false)
	private Integer dueInDays;

	@Column(nullable = false)
	private boolean active = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@Column(name = "last_generated_at")
	private LocalDate lastGeneratedAt;

	public RecurringTaskTemplate(Project project, String title, String description, TaskPriority priority,
			Set<User> assignees, RecurrenceType recurrenceType, DayOfWeek dayOfWeek, Integer dayOfMonth,
			Integer dueInDays, boolean active, User createdBy) {
		this.project = project;
		this.title = title;
		this.description = description;
		this.priority = priority;
		this.assignees = new LinkedHashSet<>(assignees);
		this.recurrenceType = recurrenceType;
		this.dayOfWeek = dayOfWeek;
		this.dayOfMonth = dayOfMonth;
		this.dueInDays = dueInDays;
		this.active = active;
		this.createdBy = createdBy;
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

	public void changeAssignees(Set<User> assignees) {
		this.assignees.clear();
		this.assignees.addAll(assignees);
	}

	/** WEEKLY/MONTHLY 상호배타 규칙을 항상 함께 바꿔 중간 상태(둘 다 값이 있거나 둘 다 없는 상태)가 생기지 않게 한다. */
	public void changeRecurrence(RecurrenceType recurrenceType, DayOfWeek dayOfWeek, Integer dayOfMonth) {
		this.recurrenceType = recurrenceType;
		this.dayOfWeek = dayOfWeek;
		this.dayOfMonth = dayOfMonth;
	}

	public void changeDueInDays(Integer dueInDays) {
		this.dueInDays = dueInDays;
	}

	public void changeActive(boolean active) {
		this.active = active;
	}

	public void markGenerated(LocalDate generatedOn) {
		this.lastGeneratedAt = generatedOn;
	}
}
