package com.teamsync.back.project;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.LastProjectMemberException;
import com.teamsync.back.common.exception.MemberNotFoundException;
import com.teamsync.back.common.exception.ProjectHasDependenciesException;
import com.teamsync.back.common.exception.ProjectMemberAlreadyExistsException;
import com.teamsync.back.common.exception.ProjectMemberNotFoundException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.RemoveProjectCreatorException;
import com.teamsync.back.project.dto.MemberSummaryResponse;
import com.teamsync.back.project.dto.ProjectAdminResponse;
import com.teamsync.back.project.dto.ProjectCreateRequest;
import com.teamsync.back.project.dto.ProjectResponse;
import com.teamsync.back.project.dto.ProjectStatsResponse;
import com.teamsync.back.project.dto.ProjectUpdateRequest;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.recurrence.RecurringTaskTemplateRepository;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import com.teamsync.back.workspace.WorkspaceRepository;
import java.util.List;
import java.util.Set;
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
	private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;
	private final ProjectMemberRepository projectMemberRepository;

	public ProjectService(ProjectRepository projectRepository, WorkspaceRepository workspaceRepository,
			UserRepository userRepository, TaskRepository taskRepository,
			RecurringTaskTemplateRepository recurringTaskTemplateRepository,
			ProjectMemberRepository projectMemberRepository) {
		this.projectRepository = projectRepository;
		this.workspaceRepository = workspaceRepository;
		this.userRepository = userRepository;
		this.taskRepository = taskRepository;
		this.recurringTaskTemplateRepository = recurringTaskTemplateRepository;
		this.projectMemberRepository = projectMemberRepository;
	}

	/**
	 * FR-301 프로젝트 멤버십 테이블(project_members) 도입: 생성자는 자동으로 첫 멤버가 된다.
	 * memberIds가 함께 전달되면(생성 화면에서 기존 구성원을 바로 선택한 경우) 같은 워크스페이스
	 * 소속인 사용자만 걸러 함께 추가한다 — 다른 워크스페이스 id나 생성자 본인 중복은 무시한다.
	 */
	@Transactional
	public ProjectResponse createProject(AuthenticatedUser principal, ProjectCreateRequest request) {
		Workspace workspace = workspaceRepository.getReferenceById(principal.workspaceId());
		User createdBy = userRepository.getReferenceById(principal.userId());

		Project project = projectRepository.save(new Project(workspace, request.name().trim(),
				request.description(), request.deadline(), createdBy));
		projectMemberRepository.save(new ProjectMember(project, createdBy));

		if (request.memberIds() != null) {
			Set<Long> uniqueMemberIds = Set.copyOf(request.memberIds());
			for (Long memberId : uniqueMemberIds) {
				if (memberId == null || memberId.equals(createdBy.getId())) {
					continue;
				}
				userRepository.findByIdAndWorkspaceId(memberId, principal.workspaceId())
						.ifPresent(user -> projectMemberRepository.save(new ProjectMember(project, user)));
			}
		}

		return ProjectResponse.from(project);
	}

	/**
	 * 프로젝트 생성 화면의 "기존 구성원 추가" 선택용 워크스페이스 전체 사용자 목록(자기 자신 제외).
	 * 생성 전이라 project_members가 아직 없으므로 listCandidateMembers와 달리 프로젝트 기준 제외는
	 * 하지 않는다. POST /api/projects와 동일한 role만 호출 가능하다(컨트롤러 @PreAuthorize).
	 */
	@Transactional(readOnly = true)
	public List<MemberSummaryResponse> listWorkspaceMemberCandidates(AuthenticatedUser principal) {
		return userRepository.findAllByWorkspaceIdOrderByNameAsc(principal.workspaceId()).stream()
				.filter(user -> !user.getId().equals(principal.userId()))
				.map(MemberSummaryResponse::from)
				.toList();
	}

	/**
	 * 로그인 사용자가 실제로 참여 중인(project_members) 프로젝트만 반환한다(워크스페이스 전체 목록이 아님).
	 * ADMIN이 아닌 일반 멤버에게는 ARCHIVED(보관됨) 프로젝트를 목록에서 숨긴다(관리자 프로젝트 관리 화면에서만
	 * 보관 프로젝트를 다룬다). ADMIN은 이 제약 없이 그대로 모두 볼 수 있다.
	 */
	@Transactional(readOnly = true)
	public List<ProjectResponse> listProjects(AuthenticatedUser principal) {
		boolean isAdmin = principal.role() == Role.ADMIN;
		return projectRepository
				.findAllByWorkspaceIdAndMemberUserId(principal.workspaceId(), principal.userId()).stream()
				.filter(project -> isAdmin || project.getStatus() != ProjectStatus.ARCHIVED)
				.map(ProjectResponse::from)
				.toList();
	}

	/**
	 * FR-4.2 프로젝트 리스트 및 상세(GET /api/projects/{projectId}): listMembers()와 동일하게
	 * 워크스페이스 스코핑으로 조회하며, 다른 워크스페이스의 projectId는 404(ProjectNotFoundException)로 처리한다.
	 * ADMIN이 아닌 일반 멤버가 ARCHIVED(보관됨) 프로젝트를 조회하면 존재하지 않는 것처럼 동일하게
	 * 404(ProjectNotFoundException)로 처리한다(관리자만 보관 프로젝트 상세를 볼 수 있다).
	 */
	@Transactional(readOnly = true)
	public ProjectResponse getProject(AuthenticatedUser principal, Long projectId) {
		Project project = projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
		if (project.getStatus() == ProjectStatus.ARCHIVED && principal.role() != Role.ADMIN) {
			throw new ProjectNotFoundException();
		}
		return ProjectResponse.from(project);
	}

	/**
	 * FR-301 담당자 선택용 선행 요구사항(GET /api/projects/{projectId}/members): project_members
	 * 테이블에 실제로 등록된 멤버만 반환한다(더 이상 workspace 전체 User로 근사하지 않는다). 조회 전용이라
	 * role 제한 없이 인증된 누구나(GUEST 포함) 호출 가능하다(컨트롤러에 @PreAuthorize 없음).
	 */
	@Transactional(readOnly = true)
	public List<MemberSummaryResponse> listMembers(AuthenticatedUser principal, Long projectId) {
		projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
		return projectMemberRepository.findAllByProject_IdOrderByUser_NameAsc(projectId).stream()
				.map(ProjectMember::getUser)
				.map(MemberSummaryResponse::from)
				.toList();
	}

	/**
	 * FR-301 프로젝트 멤버 추가용 후보 목록(GET /api/projects/{projectId}/members/candidates):
	 * workspace 전체 User 중 아직 해당 프로젝트 멤버가 아닌 사용자만 반환한다. ADMIN/LEADER만 호출 가능.
	 */
	@Transactional(readOnly = true)
	public List<MemberSummaryResponse> listCandidateMembers(AuthenticatedUser principal, Long projectId) {
		projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
		Set<Long> existingMemberIds = Set.copyOf(projectMemberRepository.findUserIdsByProject_Id(projectId));
		return userRepository.findAllByWorkspaceIdOrderByNameAsc(principal.workspaceId()).stream()
				.filter(user -> !existingMemberIds.contains(user.getId()))
				.map(MemberSummaryResponse::from)
				.toList();
	}

	/**
	 * FR-301 프로젝트 멤버 추가(POST /api/projects/{projectId}/members): 대상 사용자가 같은
	 * workspace 소속인지 검증 후 project_members에 추가한다. ADMIN/LEADER만 호출 가능.
	 */
	@Transactional
	public MemberSummaryResponse addMember(AuthenticatedUser principal, Long projectId, Long userId) {
		projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
		User user = userRepository.findByIdAndWorkspaceId(userId, principal.workspaceId())
				.orElseThrow(MemberNotFoundException::new);
		if (projectMemberRepository.existsByProject_IdAndUser_Id(projectId, userId)) {
			throw new ProjectMemberAlreadyExistsException();
		}
		Project projectRef = projectRepository.getReferenceById(projectId);
		projectMemberRepository.save(new ProjectMember(projectRef, user));
		return MemberSummaryResponse.from(user);
	}

	/**
	 * FR-301 프로젝트 멤버 제거(DELETE /api/projects/{projectId}/members/{userId}): 프로젝트
	 * 생성자는 제거할 수 없고, 마지막 남은 멤버도 제거할 수 없다(빈 프로젝트 방지). ADMIN/LEADER만 호출 가능.
	 */
	@Transactional
	public void removeMember(AuthenticatedUser principal, Long projectId, Long userId) {
		Project project = projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
		ProjectMember member = projectMemberRepository.findByProject_IdAndUser_Id(projectId, userId)
				.orElseThrow(ProjectMemberNotFoundException::new);
		if (project.getCreatedBy() != null && project.getCreatedBy().getId().equals(userId)) {
			throw new RemoveProjectCreatorException();
		}
		if (projectMemberRepository.countByProject_Id(projectId) <= 1) {
			throw new LastProjectMemberException();
		}
		projectMemberRepository.delete(member);
	}

	/**
	 * 프로젝트 관리(관리자, P2): GET /api/admin/projects. memberCount는 project_members 테이블의
	 * 실제 등록 인원 수다(더 이상 workspace 전체 User 수로 근사하지 않는다).
	 */
	@Transactional(readOnly = true)
	public List<ProjectAdminResponse> listProjectsForAdmin(AuthenticatedUser principal) {
		return projectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(principal.workspaceId()).stream()
				.map(project -> ProjectAdminResponse.of(project, projectMemberRepository.countByProject_Id(project.getId())))
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
		long memberCount = projectMemberRepository.countByProject_Id(projectId);
		return ProjectAdminResponse.of(project, memberCount);
	}

	/** 프로젝트 관리(관리자, P2): PATCH /api/admin/projects/{id}(이름/설명/마감일 수정). */
	@Transactional
	public ProjectAdminResponse updateProject(AuthenticatedUser principal, Long projectId,
			ProjectUpdateRequest request) {
		Project project = projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
		project.update(request.name().trim(), request.description(), request.deadline());
		long memberCount = projectMemberRepository.countByProject_Id(projectId);
		return ProjectAdminResponse.of(project, memberCount);
	}

	/**
	 * 프로젝트 관리(관리자, P2): DELETE /api/admin/projects/{id}.
	 * Task/RecurringTaskTemplate은 projects.id를 참조하는 FK이며 ON DELETE 정책이 미지정(RESTRICT)이다.
	 * 실제 삭제를 시도해 DataIntegrityViolationException을 catch-all(500)로 흘려보내는 대신, 삭제 전에
	 * 연관 데이터 존재 여부를 선제적으로 검증해 409 CONFLICT로 명확히 응답한다. WeeklyReport(V23 재설계)는
	 * 더 이상 project_id를 참조하지 않으므로(사람+주차 단위) 이 체크 대상에서 제외한다.
	 */
	@Transactional
	public void deleteProject(AuthenticatedUser principal, Long projectId) {
		Project project = projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
		if (hasDependencies(projectId)) {
			throw new ProjectHasDependenciesException();
		}
		// project_members는 ON DELETE 정책이 없으므로(V26) 프로젝트 삭제 전에 멤버십 행을 먼저 정리한다.
		projectMemberRepository.deleteByProject_Id(projectId);
		projectRepository.delete(project);
	}

	private boolean hasDependencies(Long projectId) {
		return taskRepository.existsByProject_Id(projectId)
				|| recurringTaskTemplateRepository.existsByProject_Id(projectId);
	}
}
