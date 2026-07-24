package com.teamsync.back.project;

/**
 * 프로젝트 관리(관리자, P2): 프로젝트 진행 상태.
 * ACTIVE(진행중) -> PLANNED(계획중) -> ARCHIVED(보관됨)로 관리자가 수동 전환한다.
 * 신규 생성된 프로젝트는 항상 ACTIVE로 시작한다.
 */
public enum ProjectStatus {
	ACTIVE,
	PLANNED,
	ARCHIVED
}
