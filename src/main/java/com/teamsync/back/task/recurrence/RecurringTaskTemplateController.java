package com.teamsync.back.task.recurrence;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.task.recurrence.dto.RecurringTaskTemplateCreateRequest;
import com.teamsync.back.task.recurrence.dto.RecurringTaskTemplateResponse;
import com.teamsync.back.task.recurrence.dto.RecurringTaskTemplateUpdateRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-106(반복 태스크 템플릿) API.
 * 조회(GET)는 인증된 워크스페이스 구성원이면 GUEST를 포함해 누구나 가능하고,
 * 생성/수정/삭제는 ADMIN/LEADER만 가능하다(계약 문서 명시 — TaskController의
 * ADMIN/LEADER/MEMBER 조합과 달리 MEMBER는 제외).
 */
@RestController
public class RecurringTaskTemplateController {

	private final RecurringTaskTemplateService recurringTaskTemplateService;

	public RecurringTaskTemplateController(RecurringTaskTemplateService recurringTaskTemplateService) {
		this.recurringTaskTemplateService = recurringTaskTemplateService;
	}

	@PostMapping("/api/projects/{projectId}/recurring-task-templates")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<RecurringTaskTemplateResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @Valid @RequestBody RecurringTaskTemplateCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(recurringTaskTemplateService.createTemplate(principal, projectId, request));
	}

	@GetMapping("/api/projects/{projectId}/recurring-task-templates")
	public ResponseEntity<List<RecurringTaskTemplateResponse>> list(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long projectId) {
		return ResponseEntity.ok(recurringTaskTemplateService.listTemplates(principal, projectId));
	}

	@GetMapping("/api/recurring-task-templates/{templateId}")
	public ResponseEntity<RecurringTaskTemplateResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long templateId) {
		return ResponseEntity.ok(recurringTaskTemplateService.getTemplate(principal, templateId));
	}

	@PatchMapping("/api/recurring-task-templates/{templateId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<RecurringTaskTemplateResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long templateId, @Valid @RequestBody RecurringTaskTemplateUpdateRequest request) {
		return ResponseEntity.ok(recurringTaskTemplateService.updateTemplate(principal, templateId, request));
	}

	@DeleteMapping("/api/recurring-task-templates/{templateId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long templateId) {
		recurringTaskTemplateService.deleteTemplate(principal, templateId);
		return ResponseEntity.noContent().build();
	}
}
