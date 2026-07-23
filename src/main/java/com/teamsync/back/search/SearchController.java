package com.teamsync.back.search;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.search.dto.SearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-004(워크스페이스 전체 통합 검색) API.
 * 다른 읽기 전용 목록 조회 엔드포인트(TaskController#list 등)와 동일하게 별도 @PreAuthorize 없이
 * 인증 여부만 검증한다(GUEST 포함 인증된 누구나 호출 가능, 워크스페이스 스코핑만으로 격리).
 * q 파라미터는 required=false로 받아, 파라미터 자체가 없는 경우와 공백만 있는 경우를 SearchService에서
 * 동일한 InvalidSearchRequestException(400 VALIDATION_ERROR)으로 일관되게 처리한다.
 */
@RestController
public class SearchController {

	private final SearchService searchService;

	public SearchController(SearchService searchService) {
		this.searchService = searchService;
	}

	@GetMapping("/api/search")
	public ResponseEntity<SearchResponse> search(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(name = "q", required = false) String q) {
		return ResponseEntity.ok(searchService.search(principal, q));
	}
}
