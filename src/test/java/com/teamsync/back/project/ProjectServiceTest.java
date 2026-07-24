package com.teamsync.back.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.teamsync.back.archive.ArchiveItemRepository;
import com.teamsync.back.archive.file.ArchivedFileRepository;
import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.ChannelRepository;
import com.teamsync.back.common.exception.ProjectHasDependenciesException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.report.TeamWeeklyReportRepository;
import com.teamsync.back.report.WeeklyReportRepository;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.recurrence.RecurringTaskTemplateRepository;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import com.teamsync.back.workspace.WorkspaceRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 프로젝트 관리(관리자, P2) 핵심 가드레일 단위 테스트: 워크스페이스 스코핑, 상태 변경, 삭제.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private WorkspaceRepository workspaceRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private ChannelRepository channelRepository;

	@Mock
	private ArchiveItemRepository archiveItemRepository;

	@Mock
	private ArchivedFileRepository archivedFileRepository;

	@Mock
	private WeeklyReportRepository weeklyReportRepository;

	@Mock
	private TeamWeeklyReportRepository teamWeeklyReportRepository;

	@Mock
	private RecurringTaskTemplateRepository recurringTaskTemplateRepository;

	private ProjectService projectService;
	private Workspace workspace;
	private AuthenticatedUser adminPrincipal;

	@BeforeEach
	void setUp() throws Exception {
		projectService = new ProjectService(projectRepository, workspaceRepository, userRepository, taskRepository,
				channelRepository, archiveItemRepository, archivedFileRepository, weeklyReportRepository,
				teamWeeklyReportRepository, recurringTaskTemplateRepository);
		workspace = new Workspace("그로우테크", "growtech.io");
		setId(workspace, 10L);
		adminPrincipal = new AuthenticatedUser(1L, 10L, "admin@growtech.io", Role.ADMIN);
	}

	@Test
	void 관리자_목록_조회시_memberCount는_워크스페이스_전체_User_수로_근사한다() throws Exception {
		Project project = newProject("알파", workspace);
		setId(project, 100L);
		when(projectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(project));
		when(userRepository.countByWorkspaceId(10L)).thenReturn(5L);

		var result = projectService.listProjectsForAdmin(adminPrincipal);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).memberCount()).isEqualTo(5L);
		assertThat(result.get(0).status()).isEqualTo("ACTIVE");
	}

	@Test
	void 통계는_status별로_집계된다() {
		when(projectRepository.countByWorkspaceId(10L)).thenReturn(6L);
		when(projectRepository.countByWorkspaceIdAndStatus(10L, ProjectStatus.ACTIVE)).thenReturn(3L);
		when(projectRepository.countByWorkspaceIdAndStatus(10L, ProjectStatus.PLANNED)).thenReturn(2L);
		when(projectRepository.countByWorkspaceIdAndStatus(10L, ProjectStatus.ARCHIVED)).thenReturn(1L);

		var stats = projectService.getStats(adminPrincipal);

		assertThat(stats.total()).isEqualTo(6L);
		assertThat(stats.active()).isEqualTo(3L);
		assertThat(stats.planned()).isEqualTo(2L);
		assertThat(stats.archived()).isEqualTo(1L);
	}

	@Test
	void 상태_변경은_같은_워크스페이스_프로젝트에만_적용된다() throws Exception {
		Project project = newProject("베타", workspace);
		setId(project, 200L);
		when(projectRepository.findByIdAndWorkspaceId(200L, 10L)).thenReturn(Optional.of(project));
		when(userRepository.countByWorkspaceId(10L)).thenReturn(3L);

		var result = projectService.changeStatus(adminPrincipal, 200L, ProjectStatus.ARCHIVED);

		assertThat(result.status()).isEqualTo("ARCHIVED");
	}

	@Test
	void 다른_워크스페이스_프로젝트_상태_변경시_예외() {
		when(projectRepository.findByIdAndWorkspaceId(999L, 10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectService.changeStatus(adminPrincipal, 999L, ProjectStatus.ARCHIVED))
				.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void 다른_워크스페이스_프로젝트_삭제시_예외() {
		when(projectRepository.findByIdAndWorkspaceId(999L, 10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectService.deleteProject(adminPrincipal, 999L))
				.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void 연관된_태스크가_있는_프로젝트_삭제시_409_예외이고_실제_삭제는_호출되지_않는다() throws Exception {
		Project project = newProject("감마", workspace);
		setId(project, 300L);
		when(projectRepository.findByIdAndWorkspaceId(300L, 10L)).thenReturn(Optional.of(project));
		when(taskRepository.existsByProject_Id(300L)).thenReturn(true);

		assertThatThrownBy(() -> projectService.deleteProject(adminPrincipal, 300L))
				.isInstanceOf(ProjectHasDependenciesException.class);

		org.mockito.Mockito.verify(projectRepository, org.mockito.Mockito.never()).delete(project);
	}

	@Test
	void 연관_데이터가_없는_프로젝트는_정상_삭제된다() throws Exception {
		Project project = newProject("델타", workspace);
		setId(project, 400L);
		when(projectRepository.findByIdAndWorkspaceId(400L, 10L)).thenReturn(Optional.of(project));

		projectService.deleteProject(adminPrincipal, 400L);

		org.mockito.Mockito.verify(projectRepository).delete(project);
	}

	private Project newProject(String name, Workspace workspace) {
		return new Project(workspace, name, "설명", null);
	}

	private void setId(Object entity, Long id) throws Exception {
		Field idField = entity.getClass().getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(entity, id);
	}
}
