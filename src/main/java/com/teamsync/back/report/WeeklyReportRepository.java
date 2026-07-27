package com.teamsync.back.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

	// GET/PUT /reports/me/entries, POST /reports/me/submit 공통: 없으면 서비스 계층에서 자동 생성한다.
	Optional<WeeklyReport> findByUser_IdAndWeekStart(Long userId, LocalDate weekStart);

	// GET /reports/team, GET /reports/executive: 해당 주 워크스페이스 전체 개인 보고서 조회.
	List<WeeklyReport> findAllByUser_IdInAndWeekStart(List<Long> userIds, LocalDate weekStart);

	long countByUser_IdInAndWeekStartAndStatus(List<Long> userIds, LocalDate weekStart, WeeklyReportStatus status);

	// GET /reports/history(FR-410): "보고서 생성이 시작된 시점부터"의 과거 주차 목록을 구성하기 위한
	// distinct weekStart. 워크스페이스 스코핑은 user.workspace로 조인한다(WeeklyReport는 더 이상
	// project에 종속되지 않으므로).
	@Query("SELECT DISTINCT wr.weekStart FROM WeeklyReport wr WHERE wr.user.workspace.id = :workspaceId")
	List<LocalDate> findDistinctWeekStartsByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
