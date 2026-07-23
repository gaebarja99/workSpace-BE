package com.teamsync.back.archive;

/**
 * FR-205 정보 아카이브 항목 유형. DECISION(결정사항), REFERENCE(참고자료).
 * 타입별 필터링은 프론트가 전체 목록을 받아 클라이언트에서 처리하므로 서버 API에는
 * 타입 기준 쿼리 파라미터를 두지 않는다.
 */
public enum ArchiveItemType {
	DECISION,
	REFERENCE
}
