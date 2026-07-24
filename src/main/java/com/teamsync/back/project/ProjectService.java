package com.teamsync.back.project;

import com.teamsync.back.archive.ArchiveItemRepository;
import com.teamsync.back.archive.file.ArchivedFileRepository;
import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.ChannelRepository;
import com.teamsync.back.common.exception.ProjectHasDependenciesException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.project.dto.MemberSummaryResponse;
import com.teamsync.back.project.dto.ProjectAdminResponse;
import com.teamsync.back.project.dto.ProjectCreateRequest;
import com.teamsync.back.project.dto.ProjectResponse;
import com.teamsync.back.project.dto.ProjectStatsResponse;
import com.teamsync.back.report.TeamWeeklyReportRepository;
import com.teamsync.back.report.WeeklyReportRepository;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.recurrence.RecurringTaskTemplateRepository;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import com.teamsync.back.workspace.WorkspaceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-001 프로젝트 최소 골격 서비스.
 * 리스크 대응(PRD 5.6, 워크스페이스 도메인 인증 오류로 인한 타 조직 데이터 접근 방지):
 * 클라이언트가 workspaceId를 직접 지정하지 않고, 항상 JWT(AuthenticatedUser)에서 추출한
 * 워크스페이스로만 조회/생성을 스코핑한다.
 */
@Service
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final WorkspaceRepository workspaceRepository;
	private final UserRepository userRepository;
	private final TaskRepository taskRepository;
	private final ChannelRepository channelRepository;
	private final ArchiveItemRepository archiveItemRepository;
	private final ArchivedFileRepository archivedFileRepository;
	private final WeeklyReportRepository weeklyReportRepository;
	private final TeamWeeklyReportRepository teamWeeklyReportRepository;
	private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;

	public ProjectService(ProjectRepository projectRepository, WorkspaceRepository workspaceRepository,
			UserRepository userRepository, TaskRepository taskRepository, ChannelRepository channelRepository,
			ArchiveItemRepository archiveItemRepository, ArchivedFileRepository archivedFileRepository,
			WeeklyReportRepository weeklyReportRepository, TeamWeeklyReportRepository teamWeeklyReportRepository,
			RecurringTaskTemplateRepository recurringTaskTemplateRepository) {
		this.projectRepository = projectRepository;
		this.workspaceRepository = workspaceRepository;
		this.userRepository = userRepository;
		this.taskRepository = taskRepository;
		this.channelRepository = channelRepository;
		this.archiveItemRepository = archiveItemRepository;
		this.archivedFileRepository = archivedFileRepository;
		this.weeklyReportRepository = weeklyReportRepository;
		this.teamWeeklyReportRepository = teamWeeklyReportRepository;
		this.recurringTaskTemplateRepository = recurringTaskTemplateRepository;
	}

	@Transactional
	public ProjectResponse createProject(AuthenticatedUser principal, ProjectCreateRequest request) {
		Workspace workspace = workspaceRepository.getReferenceById(principal.workspaceId());
		User createdBy = userRepository.getReferenceById(principal.userId());

		Project project = projectRepository.save(
				new Project(workspace, request.name().trim(), request.description(), createdBy));

		return ProjectResponse.from(project);
	}

	@Transactional(readOnly = true)
	public List<ProjectResponse> listProjects(AuthenticatedUser principal) {
		return projectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(principal.workspaceId()).stream()
				.map(ProjectResponse::from)
				.toList();
	}

	/**
	 * FR-301 담당자 선택용 선행 요구사항(GET /api/projects/{projectId}/members): 프로젝트별 멤버십
	 * 테이블이 없으므로 project가 속한 workspace의 모든 User를 "멤버"로 반환한다. 조회 전용이라 role
	 * 제한 없이 인증된 누구나(GUEST 포함) 호출 가능하다(컨트롤러에 @PreAuthorize 없음).
	 */
	@Transactional(readOnly = true)
	public List<MemberSummaryResponse> listMembers(AuthenticatedUser principal, Long projectId) {
		projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
		return userRepository.findAllByWorkspaceIdOrderByNameAsc(principal.workspaceId()).stream()
				.map(MemberSummaryResponse::from)
				.toList();
	}

	/**
	 * 프로젝트 관리(관리자, P2): GET /api/admin/projects. memberCount는 프로젝트별 멤버십 테이블이
	 * 없으므로 listMembers()와 동일하게 workspace 전체 User 수로 근사한다.
	 */
	@Transactional(readOnly = true)
	public List<ProjectAdminResponse> listProjectsForAdmin(AuthenticatedUser principal) {
		long memberCount = userRepository.countByWorkspaceId(principal.workspaceId());
		return projectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(principal.workspaceId()).stream()
				.map(project -> ProjectAdminResponse.of(project, memberCount))
				.toList();
	}

	/** 프로젝트 관리(관리자, P2): GET /api/admin/projects/stats. */
	@Transactional(readOnly = true)
	public ProjectStatsResponse getStats(AuthenticatedUser principal) {
		Long workspaceId = principal.workspaceId();
		long total = projectRepository.countByWorkspaceId(workspaceId);
		long active = projectRepository.countByWorkspaceIdAndStatus(workspaceId, ProjectStatus.ACTIVE);
		long planned = projectRepository.countByWorkspaceIdAndStatus(workspaceId, ProjectStatus.PLANNED);
		long archived = projectRepository.countByWorkspaceIdAndStatus(workspaceId, ProjectStatus.ARCHIVED);
		return new ProjectStatsResponse(total, active, planned, archived);
	}

	/** 프로젝트 관리(관리자, P2): PATCH /api/admin/projects/{id}/status. */
	@Transactional
	public ProjectAdminResponse changeStatus(AuthenticatedUser principal, Long projectId, ProjectStatus newStatus) {
		Project project = projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
		project.changeStatus(newStatus);
		long memberCount = userRepository.countByWorkspaceId(principal.workspaceId());
		return ProjectAdminResponse.of(project, memberCount);
	}

	/**
	 * 프로젝트 관리(관리자, P2): DELETE /api/admin/projects/{id}.
	 * Task/Channel/ArchiveItem/ArchivedFile/WeeklyReport/TeamWeeklyReport/RecurringTaskTemplate은
	 * 모두 projects.id를 참조하는 FK이며 ON DELETE 정책이 미지정(RESTRICT)이다. 실제 삭제를 시도해
	 * DataIntegrityViolationException을 catch-all(500)로 흘려보내는 대신, 삭제 전에 연관 데이터
	 * 존재 여부를 선제적으로 검증해 409 CONFLICT로 명확히 응답한다.
	 */
	@Transactional
	public void deleteProject(AuthenticatedUser principal, Long projectId) {
		Project project = projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
		if (hasDependencies(projectId)) {
			throw new ProjectHasDependenciesException();
		}
		projectRepository.delete(project);
	}

	private boolean hasDependencies(Long projectId) {
		return taskRepository.existsByProject_Id(projectId)
				|| channelRepository.existsByProject_Id(projectId)
				|| archiveItemRepository.existsByProject_Id(projectId)
				|| archivedFileRepository.existsByProject_Id(projectId)
				|| weeklyReportRepository.existsByProject_Id(projectId)
				|| teamWeeklyReportRepository.existsByProject_Id(projectId)
				|| recurringTaskTemplateRepository.existsByProject_Id(projectId);
	}
}
