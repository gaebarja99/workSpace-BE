package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.report.dto.NextWeekPlanUpdateRequest;
import com.teamsync.back.report.dto.RemindResponse;
import com.teamsync.back.report.dto.ReportHistoryItem;
import com.teamsync.back.report.dto.TeamWeeklyReportResponse;
import com.teamsync.back.report.dto.WeeklyReportResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-401~404/408/410(P2 주간 보고 자동화) API. 모든 엔드포인트는 /api/projects/{projectId}/reports
 * 하위에 있으며, 개인 보고서(/me*)는 GUEST를 제외한 ADMIN/LEADER/MEMBER, 팀 보고서(/team*)는
 * ADMIN/LEADER만 호출 가능하다(계약 문서 기준). ChannelController(FR-402 하이라이트 토글)는 이 컨트롤러가
 * 아니라 채널 도메인에 그대로 둔다(핀 토글과 같은 위치/스타일 유지).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/reports")
public class ReportController {

	private final WeeklyReportService weeklyReportService;

	public ReportController(WeeklyReportService weeklyReportService) {
		this.weeklyReportService = weeklyReportService;
	}

	@GetMapping("/me")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<WeeklyReportResponse> getMyReport(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @RequestParam(required = false) LocalDate weekStart) {
		return ResponseEntity.ok(weeklyReportService.getOrCreateMyReport(principal, projectId, weekStart));
	}

	@PatchMapping("/me")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<WeeklyReportResponse> updateMyReport(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @RequestParam(required = false) LocalDate weekStart,
			@Valid @RequestBody NextWeekPlanUpdateRequest request) {
		return ResponseEntity.ok(weeklyReportService.updateNextWeekPlan(principal, projectId, weekStart, request));
	}

	@PostMapping("/me/submit")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<WeeklyReportResponse> submitMyReport(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @RequestParam(required = false) LocalDate weekStart) {
		return ResponseEntity.ok(weeklyReportService.submitMyReport(principal, projectId, weekStart));
	}

	@GetMapping("/team")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<TeamWeeklyReportResponse> getTeamReport(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @RequestParam(required = false) LocalDate weekStart) {
		return ResponseEntity.ok(weeklyReportService.getTeamReport(principal, projectId, weekStart));
	}

	@PostMapping("/team/publish")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<TeamWeeklyReportResponse> publishTeamReport(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long projectId,
			@RequestParam(required = false) LocalDate weekStart) {
		return ResponseEntity.ok(weeklyReportService.publishTeamReport(principal, projectId, weekStart));
	}

	@PostMapping("/team/remind")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<RemindResponse> remindTeam(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @RequestParam(required = false) LocalDate weekStart) {
		return ResponseEntity.ok(weeklyReportService.remindUnsubmitted(principal, projectId, weekStart));
	}

	@GetMapping("/history")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<List<ReportHistoryItem>> getHistory(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @RequestParam(required = false) LocalDate weekStart,
			@RequestParam(required = false) String q) {
		return ResponseEntity.ok(weeklyReportService.getHistory(principal, projectId, weekStart, q));
	}
}
