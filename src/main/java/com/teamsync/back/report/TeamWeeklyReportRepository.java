package com.teamsync.back.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamWeeklyReportRepository extends JpaRepository<TeamWeeklyReport, Long> {

	// GET /reports/team, POST /reports/team/publish(upsert 대상 조회): 존재 여부로 "발행 완료" 판정.
	Optional<TeamWeeklyReport> findByProject_IdAndWeekStart(Long projectId, LocalDate weekStart);

	// FR-410(보고 이력): "보고서 생성이 시작된 시점부터"의 과거 주차 목록을 구성하기 위한 distinct weekStart.
	@Query("SELECT DISTINCT t.weekStart FROM TeamWeeklyReport t WHERE t.project.id = :projectId")
	List<LocalDate> findDistinctWeekStartsByProjectId(@Param("projectId") Long projectId);
}
