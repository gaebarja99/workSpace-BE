package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-107: 새 의존관계를 추가하면 태스크 간 선후행 그래프에 순환(cycle)이 생기는 경우.
 * taskId가 이미 predecessorTaskId의 선행 태스크 계열(predecessorTaskId로 이어지는 기존 경로)에
 * 포함되어 있어, predecessorTaskId → taskId 관계를 추가하면 되돌아오는 고리가 만들어지는 상황이다.
 */
public class CircularDependencyException extends BusinessException {
	public CircularDependencyException() {
		super(HttpStatus.CONFLICT, "CIRCULAR_DEPENDENCY", "이 의존관계를 추가하면 순환 관계가 발생합니다.");
	}
}
