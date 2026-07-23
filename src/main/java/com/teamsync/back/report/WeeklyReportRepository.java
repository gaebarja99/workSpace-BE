package com.teamsync.back.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

	// GET/PATCH /reports/me, POST /reports/me/submit 공통: 없으면 서비스 계층에서 자동 생성한다.
	Optional<WeeklyReport> findByProject_IdAndUser_IdAndWeekStart(Long projectId, Long userId, LocalDate weekStart);

	// GET /reports/team, POST /reports/team/remind: 해당 주 프로젝트 전체 개인 보고서 조회.
	List<WeeklyReport> findAllByProject_IdAndWeekStart(Long projectId, LocalDate weekStart);

	long countByProject_IdAndWeekStartAndStatus(Long projectId, LocalDate weekStart, WeeklyReportStatus status);

	// FR-410(보고 이력): "보고서 생성이 시작된 시점부터"의 과거 주차 목록을 구성하기 위한 distinct weekStart.
	@Query("SELECT DISTINCT wr.weekStart FROM WeeklyReport wr WHERE wr.project.id = :projectId")
	List<LocalDate> findDistinctWeekStartsByProjectId(@Param("projectId") Long projectId);

	// FR-410(보고 이력 키워드 검색): keyword는 호출부에서 LIKE 와일드카드(%, _)를 이스케이프해 넘겨야
	// 한다(ESCAPE '\' 사용, FR-004와 동일 원칙).
	@Query("SELECT COUNT(wr) > 0 FROM WeeklyReport wr WHERE wr.project.id = :projectId AND wr.weekStart = :weekStart "
			+ "AND LOWER(wr.nextWeekPlan) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\'")
	boolean existsNextWeekPlanMatch(@Param("projectId") Long projectId, @Param("weekStart") LocalDate weekStart,
			@Param("keyword") String keyword);
}
