package com.teamsync.back.task.recurrence;

import com.teamsync.back.common.exception.RecurringTaskTemplateNotFoundException;
import com.teamsync.back.notification.NotificationService;
import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskActivityService;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.TaskStatus;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-106 배치에서 템플릿 1건에 대한 Task 생성을 전담한다. RecurringTaskSchedulerService와
 * 별도 빈으로 분리한 이유: {@link Transactional}의 {@code REQUIRES_NEW}는 프록시를 통한 외부
 * 호출에서만 적용되고 같은 클래스 내 self-invocation에서는 무시되므로(Spring AOP 프록시 한계),
 * 템플릿별 트랜잭션을 실제로 격리하려면 별도 컴포넌트로 분리해야 한다. 이렇게 하면 템플릿 B의
 * generateTask()가 실패해도 이미 커밋된 A(이전 템플릿)들의 Task 생성 및 lastGeneratedAt 갱신은
 * 롤백되지 않는다.
 *
 * templateId만 받아 이 트랜잭션 안에서 템플릿을 다시 조회하는 이유: 호출부의 순회 루프는
 * 트랜잭션 밖에서 동작하므로 넘겨받은 엔티티는 detached 상태다. RecurringTaskTemplate의
 * project/assignees/createdBy는 모두 LAZY 연관관계라 detached 엔티티로 그대로 접근하면
 * LazyInitializationException이 발생한다. 이 트랜잭션 안에서 다시 조회해 영속 상태로 만든 뒤
 * 사용해야 한다.
 */
@Service
public class RecurringTaskGenerationService {

	private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;
	private final TaskRepository taskRepository;
	private final TaskActivityService taskActivityService;
	private final NotificationService notificationService;

	public RecurringTaskGenerationService(RecurringTaskTemplateRepository recurringTaskTemplateRepository,
			TaskRepository taskRepository, TaskActivityService taskActivityService,
			NotificationService notificationService) {
		this.recurringTaskTemplateRepository = recurringTaskTemplateRepository;
		this.taskRepository = taskRepository;
		this.taskActivityService = taskActivityService;
		this.notificationService = notificationService;
	}

	/**
	 * 템플릿 1건의 Task 생성을 독립 트랜잭션(REQUIRES_NEW)으로 실행한다. 호출부인
	 * RecurringTaskSchedulerService#generateRecurringTasks()는 트랜잭션을 걸지 않고 순회만
	 * 하므로, 여기서 예외가 발생해도 이 템플릿의 변경분만 롤백되고 다른 템플릿에는 영향이 없다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void generateTask(Long templateId, LocalDate today) {
		RecurringTaskTemplate template = recurringTaskTemplateRepository.findWithDetailsById(templateId)
				.orElseThrow(RecurringTaskTemplateNotFoundException::new);
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
	}
}
