package com.teamsync.back.report.dto;

/**
 * FR-401 이슈 섹션의 종류. OVERDUE(마감 지남)와 STALE(21일 이상 정체, FR-406의 축소판)만 다룬다.
 * 하나의 태스크가 두 조건을 동시에 만족하면 issues 리스트에 각각 별도 항목(kind별 1건)으로 나타날 수 있다.
 */
public enum IssueKind {
	OVERDUE,
	STALE
}
