package com.teamsync.back.report.dto;

/**
 * FR-405: resolve API가 어떤 우선순위 단계에서 템플릿을 찾았는지 나타낸다.
 * PROJECT(프로젝트 전용) > WORKSPACE(워크스페이스 전사 기본) > SYSTEM_DEFAULT(둘 다 없어 비영속
 * 가상 기본값을 반환) 순으로 우선한다.
 */
public enum ReportTemplateScope {
	PROJECT,
	WORKSPACE,
	SYSTEM_DEFAULT
}
