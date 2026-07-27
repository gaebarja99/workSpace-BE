package com.teamsync.back.report;

/**
 * 주간 보고 대/중분류 표준 키워드({@link CategoryKeyword})의 종류. 소분류/상세업무는 표준 코드 테이블
 * 없이 자유 텍스트({@link WeeklyReportEntry#getMinorCategory()})로 입력한다.
 */
public enum CategoryType {
	MAJOR,
	MIDDLE
}
