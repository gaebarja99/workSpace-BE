package com.teamsync.back.project;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.project.dto.ProjectAdminResponse;
import com.teamsync.back.project.dto.ProjectStatsResponse;
import com.teamsync.back.project.dto.ProjectStatusChangeRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트 관리(관리자, P2): FR-001 프로젝트 CRUD 위에 워크스페이스 전체 프로젝트 목록/통계 조회와
 * 상태 변경/삭제 기능을 관리자 전용으로 얇게 추가한다. 프로젝트 단위 멤버십/역할 테이블은 이번 범위 밖이다.
 */
@RestController
@RequestMapping("/api/admin/projects")
public class ProjectAdminController {

	private final ProjectService projectService;

	public ProjectAdminController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<ProjectAdminResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(projectService.listProjectsForAdmin(principal));
	}

	@GetMapping("/stats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ProjectStatsResponse> stats(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(projectService.getStats(principal));
	}

	@PatchMapping("/{projectId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ProjectAdminResponse> changeStatus(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @Valid @RequestBody ProjectStatusChangeRequest request) {
		return ResponseEntity.ok(projectService.changeStatus(principal, projectId, request.status()));
	}

	@DeleteMapping("/{projectId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId) {
		projectService.deleteProject(principal, projectId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
