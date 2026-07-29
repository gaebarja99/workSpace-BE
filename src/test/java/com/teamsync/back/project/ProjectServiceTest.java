package com.teamsync.back.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.LastProjectMemberException;
import com.teamsync.back.common.exception.MemberNotFoundException;
import com.teamsync.back.common.exception.ProjectHasDependenciesException;
import com.teamsync.back.common.exception.ProjectMemberAlreadyExistsException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.RemoveProjectCreatorException;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.recurrence.RecurringTaskTemplateRepository;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import com.teamsync.back.workspace.WorkspaceRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
	private RecurringTaskTemplateRepository recurringTaskTemplateRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	private ProjectService projectService;
	private Workspace workspace;
	private AuthenticatedUser adminPrincipal;

	@BeforeEach
	void setUp() throws Exception {
		projectService = new ProjectService(projectRepository, workspaceRepository, userRepository, taskRepository,
				recurringTaskTemplateRepository, projectMemberRepository);
		workspace = new Workspace("그로우테크", "growtech.io");
		setId(workspace, 10L);
		adminPrincipal = new AuthenticatedUser(1L, 10L, "admin@growtech.io", Role.ADMIN);
	}

	@Test
	void 프로젝트_목록_조회는_로그인_사용자가_참여중인_프로젝트만_반환한다() throws Exception {
		Project project = newProject("알파", workspace);
		setId(project, 100L);
		when(projectRepository.findAllByWorkspaceIdAndMemberUserId(10L, 1L)).thenReturn(List.of(project));

		var result = projectService.listProjects(adminPrincipal);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).id()).isEqualTo(100L);
		org.mockito.Mockito.verify(projectRepository, org.mockito.Mockito.never())
				.findAllByWorkspaceIdOrderByCreatedAtDesc(10L);
	}

	@Test
	void 일반_멤버는_목록_조회시_보관된_프로젝트가_제외된다() throws Exception {
		AuthenticatedUser memberPrincipal = new AuthenticatedUser(2L, 10L, "member@growtech.io", Role.STAFF);
		Project active = newProject("알파", workspace);
		setId(active, 100L);
		Project archived = newProject("베타", workspace);
		setId(archived, 101L);
		archived.changeStatus(ProjectStatus.ARCHIVED);
		when(projectRepository.findAllByWorkspaceIdAndMemberUserId(10L, 2L)).thenReturn(List.of(active, archived));

		var result = projectService.listProjects(memberPrincipal);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).id()).isEqualTo(100L);
	}

	@Test
	void 관리자는_목록_조회시_보관된_프로젝트도_포함된다() throws Exception {
		Project active = newProject("알파", workspace);
		setId(active, 100L);
		Project archived = newProject("베타", workspace);
		setId(archived, 101L);
		archived.changeStatus(ProjectStatus.ARCHIVED);
		when(projectRepository.findAllByWorkspaceIdAndMemberUserId(10L, 1L)).thenReturn(List.of(active, archived));

		var result = projectService.listProjects(adminPrincipal);

		assertThat(result).hasSize(2);
	}

	@Test
	void 관리자_목록_조회시_memberCount는_project_members_실제_등록_인원수다() throws Exception {
		Project project = newProject("알파", workspace);
		setId(project, 100L);
		when(projectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(project));
		when(projectMemberRepository.countByProject_Id(100L)).thenReturn(5L);

		var result = projectService.listProjectsForAdmin(adminPrincipal);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).memberCount()).isEqualTo(5L);
		assertThat(result.get(0).status()).isEqualTo("ACTIVE");
	}

	@Test
	void 프로젝트_생성시_생성자가_자동으로_첫_멤버로_등록된다() throws Exception {
		User creator = new User(workspace, "admin@growtech.io", "hash", "관리자", Role.ADMIN);
		setId(creator, 1L);
		when(workspaceRepository.getReferenceById(10L)).thenReturn(workspace);
		when(userRepository.getReferenceById(1L)).thenReturn(creator);
		Project saved = newProject("알파", workspace);
		setId(saved, 100L);
		when(projectRepository.save(org.mockito.ArgumentMatchers.any(Project.class))).thenReturn(saved);

		projectService.createProject(adminPrincipal,
				new com.teamsync.back.project.dto.ProjectCreateRequest("알파", "설명", null, null));

		ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
		org.mockito.Mockito.verify(projectMemberRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isEqualTo(creator);
		assertThat(captor.getValue().getProject()).isEqualTo(saved);
	}

	@Test
	void 프로젝트_생성시_memberIds에_포함된_같은_워크스페이스_사용자도_함께_추가된다() throws Exception {
		User creator = new User(workspace, "admin@growtech.io", "hash", "관리자", Role.ADMIN);
		setId(creator, 1L);
		User invitee = new User(workspace, "member@growtech.io", "hash", "멤버", Role.STAFF);
		setId(invitee, 2L);
		when(workspaceRepository.getReferenceById(10L)).thenReturn(workspace);
		when(userRepository.getReferenceById(1L)).thenReturn(creator);
		when(userRepository.findByIdAndWorkspaceId(2L, 10L)).thenReturn(Optional.of(invitee));
		Project saved = newProject("알파", workspace);
		setId(saved, 100L);
		when(projectRepository.save(org.mockito.ArgumentMatchers.any(Project.class))).thenReturn(saved);

		projectService.createProject(adminPrincipal,
				new com.teamsync.back.project.dto.ProjectCreateRequest("알파", "설명", null, List.of(1L, 2L)));

		ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
		org.mockito.Mockito.verify(projectMemberRepository, org.mockito.Mockito.times(2)).save(captor.capture());
		assertThat(captor.getAllValues()).extracting(ProjectMember::getUser).containsExactlyInAnyOrder(creator,
				invitee);
	}

	@Test
	void 멤버_목록_조회는_실제_project_members만_반환한다() throws Exception {
		Project project = newProject("알파", workspace);
		setId(project, 100L);
		User member = new User(workspace, "member@growtech.io", "hash", "멤버", Role.STAFF);
		setId(member, 2L);
		ProjectMember projectMember = new ProjectMember(project, member);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));
		when(projectMemberRepository.findAllByProject_IdOrderByUser_NameAsc(100L)).thenReturn(List.of(projectMember));

		var result = projectService.listMembers(adminPrincipal, 100L);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).userId()).isEqualTo(2L);
		org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).findAllByWorkspaceIdOrderByNameAsc(10L);
	}

	@Test
	void 멤버_추가는_같은_워크스페이스_사용자만_허용한다() throws Exception {
		Project project = newProject("알파", workspace);
		setId(project, 100L);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));
		when(userRepository.findByIdAndWorkspaceId(2L, 10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectService.addMember(adminPrincipal, 100L, 2L))
				.isInstanceOf(MemberNotFoundException.class);
	}

	@Test
	void 이미_멤버인_사용자_추가시_예외() throws Exception {
		Project project = newProject("알파", workspace);
		setId(project, 100L);
		User user = new User(workspace, "member@growtech.io", "hash", "멤버", Role.STAFF);
		setId(user, 2L);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));
		when(userRepository.findByIdAndWorkspaceId(2L, 10L)).thenReturn(Optional.of(user));
		when(projectMemberRepository.existsByProject_IdAndUser_Id(100L, 2L)).thenReturn(true);

		assertThatThrownBy(() -> projectService.addMember(adminPrincipal, 100L, 2L))
				.isInstanceOf(ProjectMemberAlreadyExistsException.class);
	}

	@Test
	void 멤버_추가_정상_케이스() throws Exception {
		Project project = newProject("알파", workspace);
		setId(project, 100L);
		User user = new User(workspace, "member@growtech.io", "hash", "멤버", Role.STAFF);
		setId(user, 2L);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));
		when(userRepository.findByIdAndWorkspaceId(2L, 10L)).thenReturn(Optional.of(user));
		when(projectMemberRepository.existsByProject_IdAndUser_Id(100L, 2L)).thenReturn(false);
		when(projectRepository.getReferenceById(100L)).thenReturn(project);

		var result = projectService.addMember(adminPrincipal, 100L, 2L);

		assertThat(result.userId()).isEqualTo(2L);
		org.mockito.Mockito.verify(projectMemberRepository).save(org.mockito.ArgumentMatchers.any(ProjectMember.class));
	}

	@Test
	void 프로젝트_생성자는_제거할_수_없다() throws Exception {
		User creator = new User(workspace, "admin@growtech.io", "hash", "관리자", Role.ADMIN);
		setId(creator, 1L);
		Project project = newProject("알파", workspace, creator);
		setId(project, 100L);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));
		when(projectMemberRepository.findByProject_IdAndUser_Id(100L, 1L))
				.thenReturn(Optional.of(new ProjectMember(project, creator)));

		assertThatThrownBy(() -> projectService.removeMember(adminPrincipal, 100L, 1L))
				.isInstanceOf(RemoveProjectCreatorException.class);
	}

	@Test
	void 마지막_멤버는_제거할_수_없다() throws Exception {
		User member = new User(workspace, "member@growtech.io", "hash", "멤버", Role.STAFF);
		setId(member, 2L);
		Project project = newProject("알파", workspace, null);
		setId(project, 100L);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));
		when(projectMemberRepository.findByProject_IdAndUser_Id(100L, 2L))
				.thenReturn(Optional.of(new ProjectMember(project, member)));
		when(projectMemberRepository.countByProject_Id(100L)).thenReturn(1L);

		assertThatThrownBy(() -> projectService.removeMember(adminPrincipal, 100L, 2L))
				.isInstanceOf(LastProjectMemberException.class);
	}

	@Test
	void 멤버_제거_정상_케이스() throws Exception {
		User member = new User(workspace, "member@growtech.io", "hash", "멤버", Role.STAFF);
		setId(member, 2L);
		Project project = newProject("알파", workspace, null);
		setId(project, 100L);
		ProjectMember projectMember = new ProjectMember(project, member);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));
		when(projectMemberRepository.findByProject_IdAndUser_Id(100L, 2L)).thenReturn(Optional.of(projectMember));
		when(projectMemberRepository.countByProject_Id(100L)).thenReturn(2L);

		projectService.removeMember(adminPrincipal, 100L, 2L);

		org.mockito.Mockito.verify(projectMemberRepository).delete(projectMember);
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
	void 단건_조회는_같은_워크스페이스_프로젝트를_반환한다() throws Exception {
		Project project = newProject("알파", workspace);
		setId(project, 100L);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));

		var result = projectService.getProject(adminPrincipal, 100L);

		assertThat(result.name()).isEqualTo("알파");
	}

	@Test
	void 일반_멤버는_보관된_프로젝트_단건_조회시_예외() throws Exception {
		AuthenticatedUser memberPrincipal = new AuthenticatedUser(2L, 10L, "member@growtech.io", Role.STAFF);
		Project archived = newProject("베타", workspace);
		setId(archived, 100L);
		archived.changeStatus(ProjectStatus.ARCHIVED);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(archived));

		assertThatThrownBy(() -> projectService.getProject(memberPrincipal, 100L))
				.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void 관리자는_보관된_프로젝트도_단건_조회할_수_있다() throws Exception {
		Project archived = newProject("베타", workspace);
		setId(archived, 100L);
		archived.changeStatus(ProjectStatus.ARCHIVED);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(archived));

		var result = projectService.getProject(adminPrincipal, 100L);

		assertThat(result.name()).isEqualTo("베타");
	}

	@Test
	void 다른_워크스페이스_프로젝트_단건_조회시_예외() {
		when(projectRepository.findByIdAndWorkspaceId(999L, 10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectService.getProject(adminPrincipal, 999L))
				.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void 상태_변경은_같은_워크스페이스_프로젝트에만_적용된다() throws Exception {
		Project project = newProject("베타", workspace);
		setId(project, 200L);
		when(projectRepository.findByIdAndWorkspaceId(200L, 10L)).thenReturn(Optional.of(project));
		when(projectMemberRepository.countByProject_Id(200L)).thenReturn(3L);

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

	private Project newProject(String name, Workspace workspace, User createdBy) {
		return new Project(workspace, name, "설명", createdBy);
	}

	private void setId(Object entity, Long id) throws Exception {
		Field idField = entity.getClass().getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(entity, id);
	}
}
