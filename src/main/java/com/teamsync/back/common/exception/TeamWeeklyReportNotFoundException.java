package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-409(보고서 내보내기): 요청한 팀 주간 보고(발행 기록)가 존재하지 않거나 다른 워크스페이스
 * 소속이라 요청자에게 보이지 않아야 하는 경우. ProjectNotFoundException과 동일 원칙으로 403이
 * 아닌 404로 응답한다(PRD 5.6 리스크 대응). 팀 보고서 내보내기 자체는 LEADER/ADMIN만 호출
 * 가능하도록 컨트롤러에서 @PreAuthorize로 제한하므로, 이 예외는 워크스페이스 불일치(혹은 아직
 * 발행되지 않아 레코드가 없는 경우)만 다룬다.
 */
public class TeamWeeklyReportNotFoundException extends BusinessException {
	public TeamWeeklyReportNotFoundException() {
		super(HttpStatus.NOT_FOUND, "TEAM_WEEKLY_REPORT_NOT_FOUND", "팀 주간 보고를 찾을 수 없습니다.");
	}
}
