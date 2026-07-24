package com.teamsync.back.task.recurrence;

import com.teamsync.back.notification.NotificationService;
import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskActivityService;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.TaskStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-106(반복 태스크 템플릿 자동 생성) 배치. NotificationService/WeeklyReportService의 기존
 * @Scheduled + @Transactional 패턴을 그대로 따른다(Quartz 아님, 시스템 전역 1일 1회).
 * 매일 새벽 01:00(KST)에 active=true인 모든 템플릿을 순회하며, 오늘이 그 템플릿의 생성일에
 * 해당하고 이번 주기(주/월)에 아직 생성하지 않았다면 Task를 1건 생성한다.
 *
 * FR-302(채널 시스템 메시지 자동 게시)는 이 배치의 범위 밖이다 — 계약 문서가 명시적으로 요구한
 * notifyTaskAssigned/recordCreated만 재사용하고, TaskService 전용 채널 게시 로직까지 확장하지
 * 않는다(임의 선행 작업 확장 금지 원칙).
 */
@Slf4j
@Service
public class RecurringTaskSchedulerService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;
	private final TaskRepository taskRepository;
	private final TaskActivityService taskActivityService;
	private final NotificationService notificationService;

	public RecurringTaskSchedulerService(RecurringTaskTemplateRepository recurringTaskTemplateRepository,
			TaskRepository taskRepository, TaskActivityService taskActivityService,
			NotificationService notificationService) {
		this.recurringTaskTemplateRepository = recurringTaskTemplateRepository;
		this.taskRepository = taskRepository;
		this.taskActivityService = taskActivityService;
		this.notificationService = notificationService;
	}

	@Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
	@Transactional
	public void generateRecurringTasks() {
		LocalDate today = LocalDate.now(KST);
		for (RecurringTaskTemplate template : recurringTaskTemplateRepository.findAllByActiveTrue()) {
			if (shouldGenerateToday(template, today)) {
				generateTask(template, today);
			}
		}
	}

	private boolean shouldGenerateToday(RecurringTaskTemplate template, LocalDate today) {
		if (template.getRecurrenceType() == RecurrenceType.WEEKLY) {
			if (today.getDayOfWeek() != template.getDayOfWeek()) {
				return false;
			}
			return !generatedWithinLast7Days(template.getLastGeneratedAt(), today);
		}

		// MONTHLY: 해당 월에 dayOfMonth가 없으면(예: 31일인데 2월) 그 달의 마지막 날로 클램핑한다.
		int lastDayOfMonth = YearMonth.from(today).lengthOfMonth();
		int targetDay = Math.min(template.getDayOfMonth(), lastDayOfMonth);
		if (today.getDayOfMonth() != targetDay) {
			return false;
		}
		return !generatedThisMonth(template.getLastGeneratedAt(), today);
	}

	/** lastGeneratedAt이 없거나(null) 오늘 기준 지난 7일(0~6일 전, 오늘 포함) 이내가 아니면 이번 주에 아직 생성되지 않은 것이다. */
	private boolean generatedWithinLast7Days(LocalDate lastGeneratedAt, LocalDate today) {
		if (lastGeneratedAt == null) {
			return false;
		}
		long daysSince = ChronoUnit.DAYS.between(lastGeneratedAt, today);
		return daysSince >= 0 && daysSince < 7;
	}

	private boolean generatedThisMonth(LocalDate lastGeneratedAt, LocalDate today) {
		return lastGeneratedAt != null
				&& lastGeneratedAt.getYear() == today.getYear()
				&& lastGeneratedAt.getMonth() == today.getMonth();
	}

	private void generateTask(RecurringTaskTemplate template, LocalDate today) {
		LocalDate dueDate = today.plusDays(template.getDueInDays());
		Task task = new Task(
				template.getProject(),
				template.getTitle(),
				template.getDescription(),
				template.getPriority(),
				TaskStatus.TODO,
				null,
				dueDate,
				template.getCreatedBy(),
				template.getAssignees(),
				template);
		Task savedTask = taskRepository.save(task);

		// 계약 문서 지시대로 기존 알림/활동로그 트리거만 그대로 재사용한다(일관성 유지). actingUserId가
		// 없는(시스템 배치) 생성이므로 담당자 전원이 알림 대상이다.
		taskActivityService.recordCreated(savedTask, template.getCreatedBy());
		notificationService.notifyTaskAssigned(savedTask, savedTask.getAssignees(), null);

		template.markGenerated(today);
		log.info("FR-106 반복 태스크를 자동 생성했습니다. templateId={}, taskId={}", template.getId(), savedTask.getId());
	}
}
