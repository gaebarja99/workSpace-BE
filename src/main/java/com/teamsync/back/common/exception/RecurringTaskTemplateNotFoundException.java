package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-106: 요청한 반복 태스크 템플릿이 존재하지 않거나 다른 워크스페이스 소속 프로젝트에 속해
 * 요청자에게 보이지 않아야 하는 경우.
 */
public class RecurringTaskTemplateNotFoundException extends BusinessException {
	public RecurringTaskTemplateNotFoundException() {
		super(HttpStatus.NOT_FOUND, "RECURRING_TASK_TEMPLATE_NOT_FOUND", "반복 태스크 템플릿을 찾을 수 없습니다.");
	}
}
