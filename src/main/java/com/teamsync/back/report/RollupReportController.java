package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.report.dto.RollupResponse;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-407(P3, 임원 조직 롤업 대시보드) API. 계약 문서(fr407-contract.md) 기준: "임원" 역할은 시스템에
 * 없으므로 구성원 관리·프로젝트 관리 관리자 기능과 동일하게 ADMIN 권한으로 대체한다. ReportController와
 * 달리 특정 프로젝트에 종속되지 않는 워크스페이스 전역 조회라 /api/reports 하위에 별도 컨트롤러로 둔다.
 */
@RestController
@RequestMapping("/api/reports")
public class RollupReportController {

	private final WeeklyReportService weeklyReportService;

	public RollupReportController(WeeklyReportService weeklyReportService) {
		this.weeklyReportService = weeklyReportService;
	}

	@GetMapping("/rollup")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<RollupResponse> getRollup(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) LocalDate weekStart) {
		return ResponseEntity.ok(weeklyReportService.getOrgRollup(principal, weekStart));
	}
}
