package com.teamsync.back.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

	// GET/PUT /reports/me/entries, POST /reports/me/submit 공통: 없으면 서비스 계층에서 자동 생성한다.
	Optional<WeeklyReport> findByUser_IdAndWeekStart(Long userId, LocalDate weekStart);

	// GET /reports/team, GET /reports/executive: 해당 주 워크스페이스 전체 개인 보고서 조회.
	List<WeeklyReport> findAllByUser_IdInAndWeekStart(List<Long> userIds, LocalDate weekStart);
}
