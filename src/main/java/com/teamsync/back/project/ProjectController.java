package com.teamsync.back.project;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.project.dto.ProjectCreateRequest;
import com.teamsync.back.project.dto.ProjectResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * FR-001 프로젝트 최소 골격 API.
 * 태스크 보드/채널/아카이브는 이번 단계 범위 밖이며, 이 컨트롤러는 Project 엔티티 생성/조회만 제공한다.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')") // 게스트는 프로젝트 생성 불가(FR-002 역할 기반 권한 기초 예시)
	public ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody ProjectCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(principal, request));
	}

	@GetMapping
	public ResponseEntity<List<ProjectResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(projectService.listProjects(principal));
	}
}
