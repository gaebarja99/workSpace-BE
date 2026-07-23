package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 프로젝트가 존재하지 않거나(혹은 다른 워크스페이스 소속이라) 요청자에게 보이지 않아야 하는 경우.
 * 다른 워크스페이스 데이터 존재 자체를 숨기기 위해 403이 아닌 404로 응답한다(PRD 5.6 리스크 대응).
 */
public class ProjectNotFoundException extends BusinessException {
	public ProjectNotFoundException() {
		super(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다.");
	}
}
