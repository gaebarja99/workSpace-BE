package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-107: 삭제(DELETE /api/tasks/{taskId}/dependencies/{dependencyId})하려는 의존관계가 존재하지
 * 않거나, 존재하더라도 taskId와 무관한(predecessor도 successor도 아닌) 경우.
 */
public class TaskDependencyNotFoundException extends BusinessException {
	public TaskDependencyNotFoundException() {
		super(HttpStatus.NOT_FOUND, "TASK_DEPENDENCY_NOT_FOUND", "의존관계를 찾을 수 없습니다.");
	}
}
