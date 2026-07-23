package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-204: 요청한 파일이 존재하지 않거나(혹은 다른 워크스페이스 프로젝트 소속이라) 요청자에게
 * 보이지 않아야 하는 경우. 다른 워크스페이스 데이터 존재 자체를 숨기기 위해 403이 아닌 404로
 * 응답한다(PRD 5.6 리스크 대응).
 */
public class ArchivedFileNotFoundException extends BusinessException {
	public ArchivedFileNotFoundException() {
		super(HttpStatus.NOT_FOUND, "ARCHIVED_FILE_NOT_FOUND", "파일을 찾을 수 없습니다.");
	}
}
