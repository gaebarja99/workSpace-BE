package com.teamsync.back.report;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

	// 워크스페이스 전사 기본 템플릿(project IS NULL)은 워크스페이스당 최대 1개(V20 부분 유니크 인덱스).
	Optional<ReportTemplate> findByWorkspace_IdAndProjectIsNull(Long workspaceId);

	// 프로젝트(팀) 전용 템플릿은 프로젝트당 최대 1개(V20 부분 유니크 인덱스).
	Optional<ReportTemplate> findByProject_Id(Long projectId);

	// 워크스페이스 스코핑 검증(PRD 5.6 리스크 대응): 다른 워크스페이스 템플릿은 조회되지 않는다.
	Optional<ReportTemplate> findByIdAndWorkspace_Id(Long id, Long workspaceId);
}
