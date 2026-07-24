package com.teamsync.back.task.issue;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamsync.back.project.Project;
import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskDependency;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;
import com.teamsync.back.workspace.Workspace;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * FR-406 판정 로직(TaskIssueDetector) 단위 테스트. OVERDUE/STALE/BLOCKED 각각과 복합 케이스를
 * 검증한다. today/staleThreshold를 고정값으로 직접 주입하므로 테스트 실행 시각에 관계없이
 * 결과가 결정적이다.
 */
class TaskIssueDetectorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);
	private static final LocalDateTime STALE_THRESHOLD = LocalDateTime.of(2026, 7, 3, 0, 0); // TODAY - 21일

	private final Workspace workspace = new Workspace("그로우테크", "growtech.io");
	private final Project project = new Project(workspace, "알파", "설명", null);

	@Test
	void DONE_상태_태스크는_어떤_이슈도_검출되지_않는다() throws Exception {
		Task task = task(TaskStatus.DONE, TODAY.minusDays(10), TODAY.minusDays(30));

		List<TaskIssueDetector.DetectedIssue> issues = TaskIssueDetector.detect(task, TODAY, STALE_THRESHOLD,
				List.of());

		assertThat(issues).isEmpty();
	}

	@Test
	void 정상_태스크는_이슈가_검출되지_않는다() throws Exception {
		// 마감이 아직 남았고 최근에 업데이트됐으며 선행 태스크도 없음.
		Task task = task(TaskStatus.IN_PROGRESS, TODAY.plusDays(5), TODAY.minusDays(1));

		List<TaskIssueDetector.DetectedIssue> issues = TaskIssueDetector.detect(task, TODAY, STALE_THRESHOLD,
				List.of());

		assertThat(issues).isEmpty();
	}

	@Test
	void 마감일이_지났고_완료되지_않았으면_OVERDUE와_지연일수가_detail에_포함된다() throws Exception {
		Task task = task(TaskStatus.IN_PROGRESS, TODAY.minusDays(3), TODAY.minusDays(1));

		List<TaskIssueDetector.DetectedIssue> issues = TaskIssueDetector.detect(task, TODAY, STALE_THRESHOLD,
				List.of());

		assertThat(issues).hasSize(1);
		assertThat(issues.get(0).kind()).isEqualTo(TaskIssueKind.OVERDUE);
		assertThat(issues.get(0).detail()).isEqualTo("3일 초과");
	}

	@Test
	void 마감일이_없으면_OVERDUE가_검출되지_않는다() throws Exception {
		Task task = task(TaskStatus.IN_PROGRESS, null, TODAY.minusDays(1));

		List<TaskIssueDetector.DetectedIssue> issues = TaskIssueDetector.detect(task, TODAY, STALE_THRESHOLD,
				List.of());

		assertThat(issues).isEmpty();
	}

	@Test
	void STALE_기간_이상_미변경이면_STALE과_미변경일수가_detail에_포함된다() throws Exception {
		// updatedAt이 STALE_THRESHOLD(2026-07-03)보다 이전 -> 2026-06-20(TODAY 기준 34일 전).
		Task task = task(TaskStatus.TODO, TODAY.plusDays(30), LocalDate.of(2026, 6, 20));

		List<TaskIssueDetector.DetectedIssue> issues = TaskIssueDetector.detect(task, TODAY, STALE_THRESHOLD,
				List.of());

		assertThat(issues).hasSize(1);
		assertThat(issues.get(0).kind()).isEqualTo(TaskIssueKind.STALE);
		assertThat(issues.get(0).detail()).isEqualTo("34일 이상 미변경");
	}

	@Test
	void 완료되지_않은_선행_태스크가_있으면_BLOCKED이고_제목이_detail에_포함된다() throws Exception {
		Task predecessor = task(TaskStatus.IN_PROGRESS, TODAY.plusDays(10), TODAY.minusDays(1));
		setTitle(predecessor, "선행작업A");
		Task task = task(TaskStatus.TODO, TODAY.plusDays(30), TODAY.minusDays(1));
		TaskDependency dependency = new TaskDependency(predecessor, task, null);

		List<TaskIssueDetector.DetectedIssue> issues = TaskIssueDetector.detect(task, TODAY, STALE_THRESHOLD,
				List.of(dependency));

		assertThat(issues).hasSize(1);
		assertThat(issues.get(0).kind()).isEqualTo(TaskIssueKind.BLOCKED);
		assertThat(issues.get(0).detail()).isEqualTo("선행 태스크 '선행작업A' 미완료");
	}

	@Test
	void 선행_태스크가_완료되었으면_BLOCKED이_검출되지_않는다() throws Exception {
		Task predecessor = task(TaskStatus.DONE, TODAY.plusDays(10), TODAY.minusDays(1));
		Task task = task(TaskStatus.TODO, TODAY.plusDays(30), TODAY.minusDays(1));
		TaskDependency dependency = new TaskDependency(predecessor, task, null);

		List<TaskIssueDetector.DetectedIssue> issues = TaskIssueDetector.detect(task, TODAY, STALE_THRESHOLD,
				List.of(dependency));

		assertThat(issues).isEmpty();
	}

	@Test
	void 여러_선행_태스크가_미완료면_제목이_콤마로_연결된다() throws Exception {
		Task predecessorA = task(TaskStatus.TODO, TODAY.plusDays(10), TODAY.minusDays(1));
		setTitle(predecessorA, "A작업");
		Task predecessorB = task(TaskStatus.IN_PROGRESS, TODAY.plusDays(10), TODAY.minusDays(1));
		setTitle(predecessorB, "B작업");
		Task task = task(TaskStatus.TODO, TODAY.plusDays(30), TODAY.minusDays(1));

		List<TaskIssueDetector.DetectedIssue> issues = TaskIssueDetector.detect(task, TODAY, STALE_THRESHOLD,
				List.of(new TaskDependency(predecessorA, task, null), new TaskDependency(predecessorB, task, null)));

		assertThat(issues).hasSize(1);
		assertThat(issues.get(0).detail()).isEqualTo("선행 태스크 'A작업, B작업' 미완료");
	}

	@Test
	void 한_태스크가_OVERDUE_STALE_BLOCKED을_동시에_만족하면_세_이슈_모두_검출된다() throws Exception {
		Task predecessor = task(TaskStatus.TODO, TODAY.plusDays(10), TODAY.minusDays(1));
		setTitle(predecessor, "선행작업");
		// 마감 2일 초과 + 마지막 수정 STALE 임계보다 이전(2026-06-01) + 선행 태스크 미완료.
		Task task = task(TaskStatus.IN_PROGRESS, TODAY.minusDays(2), LocalDate.of(2026, 6, 1));
		TaskDependency dependency = new TaskDependency(predecessor, task, null);

		List<TaskIssueDetector.DetectedIssue> issues = TaskIssueDetector.detect(task, TODAY, STALE_THRESHOLD,
				List.of(dependency));

		assertThat(issues).extracting(TaskIssueDetector.DetectedIssue::kind)
				.containsExactlyInAnyOrder(TaskIssueKind.OVERDUE, TaskIssueKind.STALE, TaskIssueKind.BLOCKED);
	}

	private Task task(TaskStatus status, LocalDate dueDate, LocalDate updatedAtDate) throws Exception {
		Task task = new Task(project, "태스크", "설명", TaskPriority.MEDIUM, status, null, dueDate, null, Set.of());
		setUpdatedAt(task, updatedAtDate.atStartOfDay());
		return task;
	}

	private void setUpdatedAt(Object entity, LocalDateTime updatedAt) throws Exception {
		Field field = entity.getClass().getSuperclass().getDeclaredField("updatedAt");
		field.setAccessible(true);
		field.set(entity, updatedAt);
	}

	private void setTitle(Task task, String title) {
		task.changeTitle(title);
	}
}
