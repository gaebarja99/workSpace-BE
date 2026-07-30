package com.teamsync.back.report.keyword.dto;

import com.teamsync.back.report.keyword.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/category-keywords 요청. 같은 type+name의 활성 항목이 이미 있으면 새로 만들지 않고
 * 기존 항목을 그대로 반환한다(계약: 프론트가 "새 항목 추가"를 안전하게 재시도할 수 있도록 409 회피).
 */
public record CategoryKeywordCreateRequest(
		@NotNull CategoryType type,
		@NotBlank String name
) {
}
