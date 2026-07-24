package com.teamsync.back.report;

/**
 * FR-405 보고서 템플릿 섹션의 종류. COMPLETED/IN_PROGRESS/HIGHLIGHTS/ISSUES는 WeeklyReportService의
 * 실시간 자동 계산 섹션과 이름을 맞춘 "표준" 키(템플릿 하나당 각 키는 최대 1개, autoFilled=true 고정)이고,
 * MANUAL은 자동 계산 없이 사용자가 자유롭게 채우는 섹션(템플릿 하나에 여러 개 허용, autoFilled=false 고정)이다.
 * 이번 세션은 템플릿 관리(CRUD)만 구현하며, 실제 주간보고 작성 화면이 이 템플릿을 읽어 반영하는 연동은
 * 후속 과제다(WeeklyReportService의 4섹션 실시간 계산 로직은 그대로 유지).
 */
public enum ReportSectionKey {
	COMPLETED,
	IN_PROGRESS,
	HIGHLIGHTS,
	ISSUES,
	MANUAL
}
