package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * assigneeIds 중 존재하지 않거나 다른 워크스페이스 소속인 사용자가 포함된 경우.
 */
public class InvalidAssigneeException extends BusinessException {
	public InvalidAssigneeException() {
		super(HttpStatus.BAD_REQUEST, "INVALID_ASSIGNEE", "유효하지 않은 담당자가 포함되어 있습니다.");
	}
}
