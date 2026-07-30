package com.teamsync.back.report.keyword;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.report.keyword.dto.CategoryKeywordCreateRequest;
import com.teamsync.back.report.keyword.dto.CategoryKeywordResponse;
import com.teamsync.back.report.keyword.dto.CategoryKeywordUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주간 보고(V23) 대/중분류 표준 코드 테이블 API. 조회/생성/수정/비활성화 모두 GUEST를 제외한 인증된
 * 워크스페이스 사용자(ADMIN/LEADER/MANAGER/ASSISTANT_MANAGER/STAFF) 전원 가능하다(관리자 전용 아님 — ReportController의
 * reportMembers() 관례와 동일하게 GUEST만 제외).
 */
@RestController
@RequestMapping("/api/category-keywords")
public class CategoryKeywordController {

	private final CategoryKeywordService categoryKeywordService;

	public CategoryKeywordController(CategoryKeywordService categoryKeywordService) {
		this.categoryKeywordService = categoryKeywordService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF')")
	public ResponseEntity<List<CategoryKeywordResponse>> list(@RequestParam CategoryType type) {
		return ResponseEntity.ok(categoryKeywordService.list(type));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF')")
	public ResponseEntity<CategoryKeywordResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody CategoryKeywordCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(categoryKeywordService.create(principal, request));
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF')")
	public ResponseEntity<CategoryKeywordResponse> rename(@PathVariable Long id,
			@Valid @RequestBody CategoryKeywordUpdateRequest request) {
		return ResponseEntity.ok(categoryKeywordService.rename(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF')")
	public ResponseEntity<Void> deactivate(@PathVariable Long id) {
		categoryKeywordService.deactivate(id);
		return ResponseEntity.noContent().build();
	}
}
