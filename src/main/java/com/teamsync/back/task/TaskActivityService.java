package com.teamsync.back.task;

import com.teamsync.back.notification.TaskStatusLabels;
import com.teamsync.back.user.User;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-105-B(태스크 활동 로그): 태스크 변경 트랜잭션과 동일 트랜잭션 안에서 활동 이력을 append 한다.
 * NotificationService와 동일한 원칙으로 호출자(TaskService)가 이미 워크스페이스 스코프를 검증한 뒤 호출한다.
 * @Transactional(기본 REQUIRED)이라 호출자의 진행 중 트랜잭션에 그대로 합류한다(생성 실패 시 함께 롤백).
 */
@Service
public class TaskActivityService {

	private final TaskActivityRepository taskActivityRepository;

	public TaskActivityService(TaskActivityRepository taskActivityRepository) {
		this.taskActivityRepository = taskActivityRepository;
	}

	@Transactional
	public void recordCreated(Task task, User actor) {
		taskActivityRepository.save(new TaskActivity(task, actor, TaskActivityType.CREATED, null));
	}

	@Transactional
	public void recordStatusChanged(Task task, TaskStatus previousStatus, TaskStatus newStatus, User actor) {
		String detail = TaskStatusLabels.of(previousStatus) + " → " + TaskStatusLabels.of(newStatus);
		taskActivityRepository.save(new TaskActivity(task, actor, TaskActivityType.STATUS_CHANGED, detail));
	}

	@Transactional
	public void recordAssigneeChanged(Task task, Collection<User> assignees, User actor) {
		String names = assignees.stream().map(User::getName).collect(Collectors.joining(", "));
		String detail = "담당자: " + (names.isEmpty() ? "없음" : names);
		taskActivityRepository.save(new TaskActivity(task, actor, TaskActivityType.ASSIGNEE_CHANGED, detail));
	}

	@Transactional
	public void recordCommentAdded(Task task, User actor) {
		taskActivityRepository.save(new TaskActivity(task, actor, TaskActivityType.COMMENT_ADDED, null));
	}
}
