package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

public class ChecklistItemNotFoundException extends BusinessException {
	public ChecklistItemNotFoundException() {
		super(HttpStatus.NOT_FOUND, "CHECKLIST_ITEM_NOT_FOUND", "체크리스트 항목을 찾을 수 없습니다.");
	}
}
