package com.teamsync.back.report;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeeklyReportEntryRepository extends JpaRepository<WeeklyReportEntry, Long> {

	List<WeeklyReportEntry> findAllByReport_IdOrderBySectionAscOrderIndexAsc(Long reportId);

	// GET /reports/team, GET /reports/executive: 여러 보고서의 entries를 한 번에 조회한다.
	List<WeeklyReportEntry> findAllByReport_IdInOrderBySectionAscOrderIndexAsc(List<Long> reportIds);

	// PUT /reports/me/entries: section 단위로 기존 행을 통째로 지우고 요청 리스트로 재생성한다
	// (derived delete 메서드 — 엔티티를 조회 후 하나씩 remove()하는 방식, @Modifying 불필요).
	void deleteAllByReport_IdAndSection(Long reportId, EntrySection section);

	// GET /reports/history(FR-410) 키워드 검색: keyword는 호출부에서 LIKE 와일드카드(%, _)를
	// 이스케이프해 넘겨야 한다(ESCAPE '\' 사용, FR-004와 동일 원칙). detail/minorCategory 둘 중
	// 하나라도 매치하면 해당 주차가 검색 결과에 포함된다.
	@Query("SELECT COUNT(e) > 0 FROM WeeklyReportEntry e WHERE e.report.user.workspace.id = :workspaceId "
			+ "AND e.report.weekStart = :weekStart AND ("
			+ "LOWER(e.detail) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' "
			+ "OR LOWER(e.minorCategory) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\')")
	boolean existsKeywordMatch(@Param("workspaceId") Long workspaceId, @Param("weekStart") LocalDate weekStart,
			@Param("keyword") String keyword);
}
