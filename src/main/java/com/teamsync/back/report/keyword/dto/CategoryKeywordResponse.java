package com.teamsync.back.report.keyword.dto;

import com.teamsync.back.report.keyword.CategoryKeyword;
import com.teamsync.back.report.keyword.CategoryType;

/** GET/POST/PATCH /api/category-keywords 공통 응답. */
public record CategoryKeywordResponse(
		Long id,
		CategoryType type,
		String name,
		int orderIndex,
		boolean active
) {
	public static CategoryKeywordResponse from(CategoryKeyword keyword) {
		return new CategoryKeywordResponse(keyword.getId(), keyword.getType(), keyword.getName(),
				keyword.getOrderIndex(), keyword.isActive());
	}
}
