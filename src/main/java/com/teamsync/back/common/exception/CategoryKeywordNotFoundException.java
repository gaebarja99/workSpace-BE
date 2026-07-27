package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 주간 보고 대/중분류 표준 키워드(PATCH/DELETE /api/category-keywords/{id})를 찾을 수 없는 경우. */
public class CategoryKeywordNotFoundException extends BusinessException {
	public CategoryKeywordNotFoundException() {
		super(HttpStatus.NOT_FOUND, "CATEGORY_KEYWORD_NOT_FOUND", "카테고리 키워드를 찾을 수 없습니다.");
	}
}
