package com.teamsync.back.task.issue;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.task.issue.dto.TaskIssueItemResponse;
import com.teamsync.back.task.issue.dto.TaskIssueListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-406(이슈/리스크 자동 플래그, 완전판) API. 조회는 GUEST를 제외한 ADMIN/LEADER/MEMBER,
 * 수동 해결(resolve)은 ADMIN/LEADER만 가능하다(계약 문서 기준, ReportController와 동일 원칙).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/issues")
public class ProjectIssueController {

	private final TaskIssueFlagService taskIssueFlagService;

	public ProjectIssueController(TaskIssueFlagService taskIssueFlagService) {
		this.taskIssueFlagService = taskIssueFlagService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<TaskIssueListResponse> list(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @RequestParam(required = false) TaskIssueStatus status,
			@RequestParam(required = false) TaskIssueKind kind) {
		return ResponseEntity.ok(taskIssueFlagService.listIssues(principal, projectId, status, kind));
	}

	@PatchMapping("/{issueId}/resolve")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<TaskIssueItemResponse> resolve(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @PathVariable Long issueId) {
		return ResponseEntity.ok(taskIssueFlagService.resolveIssue(principal, projectId, issueId));
	}
}
