package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.report.dto.EntriesReplaceRequest;
import com.teamsync.back.report.dto.ExecutiveDashboardResponse;
import com.teamsync.back.report.dto.TeamDashboardResponse;
import com.teamsync.back.report.dto.WeeklyReportResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주간 보고(V23 재설계) API. project 비종속(/api/reports/... — 더 이상 /api/projects/{projectId}/reports
 * 하위가 아님). 개인 보고서(/me*)는 GUEST를 제외한 ADMIN/LEADER/MANAGER/ASSISTANT_MANAGER/STAFF,
 * 팀장 뷰(/team)는 ADMIN/LEADER,
 * 대표 뷰(/executive)는 ADMIN만 호출 가능하다.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

	private final WeeklyReportService weeklyReportService;

	public ReportController(WeeklyReportService weeklyReportService) {
		this.weeklyReportService = weeklyReportService;
	}

	@GetMapping("/me")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF')")
	public ResponseEntity<WeeklyReportResponse> getMyReport(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) LocalDate weekStart) {
		return ResponseEntity.ok(weeklyReportService.getOrCreateMyReport(principal, weekStart));
	}

	@PutMapping("/me/entries")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF')")
	public ResponseEntity<WeeklyReportResponse> replaceMyEntries(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) LocalDate weekStart, @RequestParam EntrySection section,
			@Valid @RequestBody EntriesReplaceRequest request) {
		return ResponseEntity.ok(weeklyReportService.replaceEntries(principal, weekStart, section, request));
	}

	@PostMapping("/me/submit")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF')")
	public ResponseEntity<WeeklyReportResponse> submitMyReport(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) LocalDate weekStart) {
		return ResponseEntity.ok(weeklyReportService.submitMyReport(principal, weekStart));
	}

	@GetMapping("/team")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<TeamDashboardResponse> getTeamDashboard(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) LocalDate weekStart) {
		return ResponseEntity.ok(weeklyReportService.getTeamDashboard(principal, weekStart));
	}

	@GetMapping("/executive")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ExecutiveDashboardResponse> getExecutiveDashboard(
			@AuthenticationPrincipal AuthenticatedUser principal, @RequestParam(required = false) LocalDate weekStart) {
		return ResponseEntity.ok(weeklyReportService.getExecutiveDashboard(principal, weekStart));
	}
}
