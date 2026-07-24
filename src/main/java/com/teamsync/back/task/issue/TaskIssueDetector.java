package com.teamsync.back.task.issue;

import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskDependency;
import com.teamsync.back.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FR-406 이슈/리스크 판정 로직. OVERDUE/STALE은 FR-401(WeeklyReportService.computeAutoTaskSections,
 * STALE_DAYS_THRESHOLD=21)의 판정 공식을 그대로 재사용한다:
 * - OVERDUE: status != DONE && dueDate < 오늘.
 * - STALE: status != DONE && updatedAt < now - 21일.
 * BLOCKED(신규): status != DONE이고 선행 태스크(TaskDependency.findBySuccessorTask_Id) 중 하나
 * 이상이 완료(DONE)되지 않은 경우. 한 태스크가 여러 조건을 동시에 만족하면 kind별로 각각
 * DetectedIssue를 반환한다(WeeklyReportService의 IssueItem이 OVERDUE/STALE을 중복 집계하는 것과
 * 동일한 원칙). 리포지토리 의존 없는 순수 함수라 TaskIssueDetectorTest에서 직접 검증 가능하다.
 */
public final class TaskIssueDetector {

	private TaskIssueDetector() {
	}

	public static List<DetectedIssue> detect(Task task, LocalDate today, LocalDateTime staleThreshold,
			List<TaskDependency> predecessorLinks) {
		List<DetectedIssue> issues = new ArrayList<>();
		if (task.getStatus() == TaskStatus.DONE) {
			return issues;
		}

		if (task.getDueDate() != null && task.getDueDate().isBefore(today)) {
			long daysOverdue = ChronoUnit.DAYS.between(task.getDueDate(), today);
			issues.add(new DetectedIssue(TaskIssueKind.OVERDUE, daysOverdue + "일 초과"));
		}

		if (task.getUpdatedAt().isBefore(staleThreshold)) {
			long daysSinceUpdate = ChronoUnit.DAYS.between(task.getUpdatedAt().toLocalDate(), today);
			issues.add(new DetectedIssue(TaskIssueKind.STALE, daysSinceUpdate + "일 이상 미변경"));
		}

		List<Task> incompletePredecessors = predecessorLinks.stream()
				.map(TaskDependency::getPredecessorTask)
				.filter(predecessor -> predecessor.getStatus() != TaskStatus.DONE)
				.toList();
		if (!incompletePredecessors.isEmpty()) {
			String titles = incompletePredecessors.stream().map(Task::getTitle).collect(Collectors.joining(", "));
			issues.add(new DetectedIssue(TaskIssueKind.BLOCKED, "선행 태스크 '" + titles + "' 미완료"));
		}

		return issues;
	}

	public record DetectedIssue(TaskIssueKind kind, String detail) {
	}
}
