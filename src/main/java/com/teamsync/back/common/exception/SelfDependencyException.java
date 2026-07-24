package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-107: 태스크 자기 자신을 선행 태스크로 지정하려는 경우(predecessorTaskId == taskId).
 */
public class SelfDependencyException extends BusinessException {
	public SelfDependencyException() {
		super(HttpStatus.BAD_REQUEST, "SELF_DEPENDENCY", "태스크는 자기 자신을 선행 태스크로 지정할 수 없습니다.");
	}
}
