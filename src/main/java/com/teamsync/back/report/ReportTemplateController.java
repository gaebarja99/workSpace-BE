package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.report.dto.ReportTemplateCreateRequest;
import com.teamsync.back.report.dto.ReportTemplateResponse;
import com.teamsync.back.report.dto.ReportTemplateSectionCreateRequest;
import com.teamsync.back.report.dto.ReportTemplateSectionOrderRequest;
import com.teamsync.back.report.dto.ReportTemplateSectionUpdateRequest;
import com.teamsync.back.report.dto.ReportTemplateUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-405(P3, 보고서 템플릿 관리) API. 조회(resolve)는 인증된 워크스페이스 구성원이면 전원(GUEST 포함)
 * 가능하고, 관리(생성/수정/삭제, 섹션 CRUD/순서변경)는 RecurringTaskTemplateController(FR-106)와
 * 동일하게 ADMIN/LEADER만 가능하다. 관리자 보고서 템플릿 정적 목업 화면(admin/templates)은 이 API로
 * 흡수 통합될 예정(HANDOFF 백로그 항목).
 */
@RestController
@RequestMapping("/api/report-templates")
public class ReportTemplateController {

	private final ReportTemplateService reportTemplateService;

	public ReportTemplateController(ReportTemplateService reportTemplateService) {
		this.reportTemplateService = reportTemplateService;
	}

	@GetMapping("/resolve")
	public ResponseEntity<ReportTemplateResponse> resolve(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam Long projectId) {
		return ResponseEntity.ok(reportTemplateService.resolveTemplate(principal, projectId));
	}

	@GetMapping("/workspace")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<ReportTemplateResponse> getWorkspaceTemplate(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		ReportTemplateResponse response = reportTemplateService.getWorkspaceTemplate(principal);
		return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
	}

	@GetMapping("/project/{projectId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<ReportTemplateResponse> getProjectTemplate(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long projectId) {
		ReportTemplateResponse response = reportTemplateService.getProjectTemplate(principal, projectId);
		return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<ReportTemplateResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody ReportTemplateCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(reportTemplateService.createTemplate(principal, request));
	}

	@PatchMapping("/{templateId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<ReportTemplateResponse> updateName(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long templateId, @Valid @RequestBody ReportTemplateUpdateRequest request) {
		return ResponseEntity.ok(reportTemplateService.updateTemplateName(principal, templateId, request));
	}

	@DeleteMapping("/{templateId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long templateId) {
		reportTemplateService.deleteTemplate(principal, templateId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{templateId}/sections")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<ReportTemplateResponse> addSection(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long templateId, @Valid @RequestBody ReportTemplateSectionCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(reportTemplateService.addSection(principal, templateId, request));
	}

	@PatchMapping("/{templateId}/sections/{sectionId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<ReportTemplateResponse> updateSectionTitle(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long templateId,
			@PathVariable Long sectionId, @Valid @RequestBody ReportTemplateSectionUpdateRequest request) {
		return ResponseEntity.ok(reportTemplateService.updateSectionTitle(principal, templateId, sectionId, request));
	}

	@DeleteMapping("/{templateId}/sections/{sectionId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<ReportTemplateResponse> deleteSection(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long templateId, @PathVariable Long sectionId) {
		return ResponseEntity.ok(reportTemplateService.deleteSection(principal, templateId, sectionId));
	}

	@PutMapping("/{templateId}/sections/order")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<ReportTemplateResponse> reorderSections(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long templateId,
			@Valid @RequestBody ReportTemplateSectionOrderRequest request) {
		return ResponseEntity.ok(reportTemplateService.reorderSections(principal, templateId, request));
	}
}
