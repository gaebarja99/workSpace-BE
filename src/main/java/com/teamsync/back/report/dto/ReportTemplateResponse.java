package com.teamsync.back.report.dto;

import java.util.List;

/**
 * FR-405 템플릿 조회/생성/수정 공통 응답. id는 SYSTEM_DEFAULT(비영속 가상 기본값)인 경우에만 null이다.
 * projectId는 PROJECT scope에서는 해당 프로젝트 id, WORKSPACE scope에서는 null,
 * SYSTEM_DEFAULT는 resolve 요청에 쓰인 projectId를 그대로 돌려준다(요청 컨텍스트 참고용).
 */
public record ReportTemplateResponse(
		Long id,
		ReportTemplateScope scope,
		Long projectId,
		String name,
		List<ReportTemplateSectionResponse> sections
) {
}
