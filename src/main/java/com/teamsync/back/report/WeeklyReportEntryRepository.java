package com.teamsync.back.report;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyReportEntryRepository extends JpaRepository<WeeklyReportEntry, Long> {

	List<WeeklyReportEntry> findAllByReport_IdOrderBySectionAscOrderIndexAsc(Long reportId);

	// GET /reports/team, GET /reports/executive: 여러 보고서의 entries를 한 번에 조회한다.
	List<WeeklyReportEntry> findAllByReport_IdInOrderBySectionAscOrderIndexAsc(List<Long> reportIds);

	// PUT /reports/me/entries: section 단위로 기존 행을 통째로 지우고 요청 리스트로 재생성한다
	// (derived delete 메서드 — 엔티티를 조회 후 하나씩 remove()하는 방식, @Modifying 불필요).
	void deleteAllByReport_IdAndSection(Long reportId, EntrySection section);
}
