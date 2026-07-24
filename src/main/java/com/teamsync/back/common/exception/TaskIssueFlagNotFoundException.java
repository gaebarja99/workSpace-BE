package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-406: 조회/해결(resolve)하려는 이슈 플래그가 존재하지 않거나, 존재하더라도 요청 경로의
 * projectId 소속이 아닌 경우(다른 프로젝트/워크스페이스 데이터 존재를 숨기기 위해 404로 응답).
 */
public class TaskIssueFlagNotFoundException extends BusinessException {
	public TaskIssueFlagNotFoundException() {
		super(HttpStatus.NOT_FOUND, "TASK_ISSUE_NOT_FOUND", "이슈를 찾을 수 없습니다.");
	}
}
