package com.teamsync.back.project;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.project.dto.AddMemberRequest;
import com.teamsync.back.project.dto.MemberSummaryResponse;
import com.teamsync.back.project.dto.ProjectCreateRequest;
import com.teamsync.back.project.dto.ProjectResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF')") // 게스트는 프로젝트 생성 불가(FR-002 역할 기반 권한 기초 예시)
	public ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody ProjectCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(principal, request));
	}

	@GetMapping
	public ResponseEntity<List<ProjectResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(projectService.listProjects(principal));
	}

	// FR-4.2 프로젝트 단건 조회: listMembers와 동일하게 조회 전용이라 GUEST를 포함해 인증된 누구나 호출 가능하다.
	@GetMapping("/{projectId}")
	public ResponseEntity<ProjectResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId) {
		return ResponseEntity.ok(projectService.getProject(principal, projectId));
	}

	// 프로젝트 생성 화면의 "기존 구성원 추가" 후보 목록: 아직 프로젝트가 없으므로 워크스페이스
	// 전체 사용자(본인 제외)를 반환한다. POST 생성과 동일하게 게스트를 제외한 역할만 호출 가능하다.
	// {projectId}가 Long 경로변수라 "/members/candidates"라는 리터럴 경로가 더 구체적이므로
	// Spring이 아래 "/{projectId}/members" 패턴보다 이 매핑을 우선 선택한다.
	@GetMapping("/members/candidates")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF')")
	public ResponseEntity<List<MemberSummaryResponse>> listWorkspaceMemberCandidates(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(projectService.listWorkspaceMemberCandidates(principal));
	}

	// FR-301 담당자 선택용 선행 요구사항: 조회 전용이라 GUEST를 포함해 인증된 누구나 호출 가능하다.
	@GetMapping("/{projectId}/members")
	public ResponseEntity<List<MemberSummaryResponse>> listMembers(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long projectId) {
		return ResponseEntity.ok(projectService.listMembers(principal, projectId));
	}

	// FR-301 프로젝트 멤버 추가용 후보 목록: 실제 멤버 추가는 ADMIN/LEADER만 가능하므로 후보 조회도 동일하게 제한한다.
	@GetMapping("/{projectId}/members/candidates")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<List<MemberSummaryResponse>> listCandidateMembers(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long projectId) {
		return ResponseEntity.ok(projectService.listCandidateMembers(principal, projectId));
	}

	// FR-301 프로젝트 멤버 추가.
	@PostMapping("/{projectId}/members")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<MemberSummaryResponse> addMember(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @Valid @RequestBody AddMemberRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(projectService.addMember(principal, projectId, request.userId()));
	}

	// FR-301 프로젝트 멤버 제거.
	@DeleteMapping("/{projectId}/members/{userId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<Void> removeMember(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @PathVariable Long userId) {
		projectService.removeMember(principal, projectId, userId);
		return ResponseEntity.noContent().build();
	}
}
