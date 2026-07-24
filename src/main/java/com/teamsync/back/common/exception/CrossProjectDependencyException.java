package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-107: 선행/후행 태스크가 서로 다른 프로젝트에 속해 의존관계를 맺을 수 없는 경우.
 */
public class CrossProjectDependencyException extends BusinessException {
	public CrossProjectDependencyException() {
		super(HttpStatus.BAD_REQUEST, "CROSS_PROJECT_DEPENDENCY", "서로 다른 프로젝트의 태스크는 의존관계를 맺을 수 없습니다.");
	}
}
