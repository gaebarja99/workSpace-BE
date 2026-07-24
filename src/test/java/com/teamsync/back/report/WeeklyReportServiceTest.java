package com.teamsync.back.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.message.MessageRepository;
import com.teamsync.back.common.exception.InvalidReportRequestException;
import com.teamsync.back.notification.NotificationService;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.project.ProjectStatus;
import com.teamsync.back.report.dto.RollupResponse;
import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.TaskStatus;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FR-407(조직 롤업 대시보드, GET /api/reports/rollup) 핵심 계산 로직 단위 테스트.
 * weekStart는 항상 과거 고정 주(2020-01-06 월요일)를 사용해 "오늘" 기준 OVERDUE 판정 컷오프가
 * weekEnd+1일로 고정되도록 하여(계약 문서: overdueCutoff = min(오늘, weekEnd+1일)) 테스트 시각에
 * 관계없이 결과가 결정적이도록 한다.
 */
@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

	@Mock
	private WeeklyReportRepository weeklyReportRepository;

	@Mock
	private TeamWeeklyReportRepository teamWeeklyReportRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private NotificationService notificationService;

	private WeeklyReportService weeklyReportService;
	private Workspace workspace;
	private AuthenticatedUser adminPrincipal;

	private static final LocalDate WEEK_START = LocalDate.of(2020, 1, 6); // 고정 과거 월요일
	private static final LocalDate WEEK_END = WEEK_START.plusDays(6);

	@BeforeEach
	void setUp() throws Exception {
		weeklyReportService = new WeeklyReportService(weeklyReportRepository, teamWeeklyReportRepository,
				projectRepository, userRepository, taskRepository, messageRepository, notificationService);
		workspace = new Workspace("그로우테크", "growtech.io");
		setId(workspace, 10L);
		adminPrincipal = new AuthenticatedUser(1L, 10L, "admin@growtech.io", Role.ADMIN);
	}

	@Test
	void weekStart가_월요일이_아니면_예외() {
		LocalDate tuesday = WEEK_START.plusDays(1);

		assertThatThrownBy(() -> weeklyReportService.getOrgRollup(adminPrincipal, tuesday))
				.isInstanceOf(InvalidReportRequestException.class);
	}

	@Test
	void 팀별_완료율과_지연율은_완료_진행_이슈_태스크_수를_분모로_계산된다() throws Exception {
		Project project = newProject("개발팀 프로젝트");
		setId(project, 100L);
		when(projectRepository.findAllByWorkspaceIdAndStatusOrderByIdAsc(10L, ProjectStatus.ACTIVE))
				.thenReturn(List.of(project));

		// GUEST 1명 포함 5명 -> memberCount는 GUEST 제외 4명으로 근사.
		when(userRepository.findAllByWorkspaceIdOrderByNameAsc(10L)).thenReturn(List.of(
				newUser("관리자", Role.ADMIN), newUser("팀장", Role.LEADER),
				newUser("멤버1", Role.MEMBER), newUser("멤버2", Role.MEMBER),
				newUser("게스트", Role.GUEST)));

		when(weeklyReportRepository.countByProject_IdAndWeekStartAndStatus(100L, WEEK_START, WeeklyReportStatus.SUBMITTED))
				.thenReturn(3L);

		// 완료 2건(단순 개수만 필요하므로 목업 태스크 목록 크기로 충분).
		when(taskRepository.findAllByProject_IdAndStatusAndUpdatedAtBetween(
				eq(100L), eq(TaskStatus.DONE), any(), any()))
				.thenReturn(List.of(newTask(project, null), newTask(project, null)));

		// 진행 중(미완료) 3건 중 1건은 weekEnd+1일 이전 마감이라 OVERDUE.
		Task overdueTask = newTask(project, WEEK_END.minusDays(1));
		Task futureDueTask = newTask(project, WEEK_END.plusDays(30));
		Task noDueTask = newTask(project, null);
		when(taskRepository.findAllByProject_IdAndStatusNot(eq(100L), eq(TaskStatus.DONE)))
				.thenReturn(List.of(overdueTask, futureDueTask, noDueTask));

		RollupResponse response = weeklyReportService.getOrgRollup(adminPrincipal, WEEK_START);

		// 분모 = 완료(2) + 진행(3) + 이슈(OVERDUE 1건, STALE은 updatedAt을 최근으로 세팅해 미해당) = 6.
		// completionRate = 2/6 = 33%(반올림), overdueRate = 1/6 = 17%(반올림).
		assertThat(response.weekStart()).isEqualTo(WEEK_START);
		assertThat(response.weekEnd()).isEqualTo(WEEK_END);
		assertThat(response.teams()).hasSize(1);
		assertThat(response.teams().get(0).projectId()).isEqualTo(100L);
		assertThat(response.teams().get(0).memberCount()).isEqualTo(4);
		assertThat(response.teams().get(0).submittedCount()).isEqualTo(3);
		assertThat(response.teams().get(0).completionRate()).isEqualTo(33);
		assertThat(response.teams().get(0).overdueRate()).isEqualTo(17);
		assertThat(response.orgCompletionRate()).isEqualTo(33);
		assertThat(response.trend()).hasSize(4);
		assertThat(response.trend().get(3).weekStart()).isEqualTo(WEEK_START);
		assertThat(response.trend().get(3).completionRate()).isEqualTo(33);
	}

	@Test
	void 분모가_0이면_완료율과_지연율은_0이다() throws Exception {
		Project project = newProject("빈 프로젝트");
		setId(project, 200L);
		when(projectRepository.findAllByWorkspaceIdAndStatusOrderByIdAsc(10L, ProjectStatus.ACTIVE))
				.thenReturn(List.of(project));
		when(userRepository.findAllByWorkspaceIdOrderByNameAsc(10L)).thenReturn(List.of());
		when(weeklyReportRepository.countByProject_IdAndWeekStartAndStatus(anyLong(), eq(WEEK_START), eq(WeeklyReportStatus.SUBMITTED)))
				.thenReturn(0L);
		when(taskRepository.findAllByProject_IdAndStatusAndUpdatedAtBetween(anyLong(), eq(TaskStatus.DONE), any(), any()))
				.thenReturn(List.of());
		when(taskRepository.findAllByProject_IdAndStatusNot(anyLong(), eq(TaskStatus.DONE))).thenReturn(List.of());

		RollupResponse response = weeklyReportService.getOrgRollup(adminPrincipal, WEEK_START);

		assertThat(response.teams().get(0).completionRate()).isZero();
		assertThat(response.teams().get(0).overdueRate()).isZero();
		assertThat(response.orgCompletionRate()).isZero();
	}

	private Project newProject(String name) {
		return new Project(workspace, name, "설명", null);
	}

	private User newUser(String name, Role role) {
		return new User(workspace, name.toLowerCase() + "@growtech.io", "hash", name, role);
	}

	private Task newTask(Project project, LocalDate dueDate) throws Exception {
		Task task = new Task(project, "태스크", "설명", TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS,
				null, dueDate, null, Set.of());
		setUpdatedAt(task, LocalDateTime.now()); // STALE(21일 이상 정체) 미해당으로 최근 시각 고정.
		return task;
	}

	private void setId(Object entity, Long id) throws Exception {
		Field idField = entity.getClass().getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(entity, id);
	}

	private void setUpdatedAt(Object entity, LocalDateTime updatedAt) throws Exception {
		Field field = entity.getClass().getSuperclass().getDeclaredField("updatedAt");
		field.setAccessible(true);
		field.set(entity, updatedAt);
	}
}
