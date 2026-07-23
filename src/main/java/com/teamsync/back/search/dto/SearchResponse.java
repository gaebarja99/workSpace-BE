package com.teamsync.back.search.dto;

import java.util.List;

/**
 * FR-004(통합 검색) 응답. tasks/messages/files/users 각 배열은 카테고리별 상위 10건까지만 담고,
 * 결과가 없어도 null이 아닌 빈 배열([])로 내려준다(SearchService에서 항상 빈 리스트를 보장).
 */
public record SearchResponse(
		String query,
		List<SearchTaskResult> tasks,
		List<SearchMessageResult> messages,
		List<SearchFileResult> files,
		List<SearchUserResult> users
) {
}
