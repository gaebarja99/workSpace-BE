package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-107: 이미 동일한 (predecessorTaskId, successorTaskId) 조합의 의존관계가 존재하는 경우.
 */
public class DuplicateDependencyException extends BusinessException {
	public DuplicateDependencyException() {
		super(HttpStatus.CONFLICT, "DUPLICATE_DEPENDENCY", "이미 동일한 의존관계가 존재합니다.");
	}
}
