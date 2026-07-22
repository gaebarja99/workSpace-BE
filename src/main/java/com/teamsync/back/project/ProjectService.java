package com.teamsync.back.project;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.project.dto.ProjectCreateRequest;
import com.teamsync.back.project.dto.ProjectResponse;
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

	public ProjectService(ProjectRepository projectRepository, WorkspaceRepository workspaceRepository,
			UserRepository userRepository) {
		this.projectRepository = projectRepository;
		this.workspaceRepository = workspaceRepository;
		this.userRepository = userRepository;
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
}
