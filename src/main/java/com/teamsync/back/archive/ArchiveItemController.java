package com.teamsync.back.archive;

import com.teamsync.back.archive.dto.ArchiveItemCreateRequest;
import com.teamsync.back.archive.dto.ArchiveItemResponse;
import com.teamsync.back.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-205(정보 아카이브 — 위키형 결정사항/참고자료) API.
 * 조회(GET)는 인증된 워크스페이스 구성원이면 GUEST를 포함해 누구나 가능하고,
 * 생성(POST)은 TaskController/ChannelController와 동일하게 GUEST를 제외한 ADMIN/LEADER/MEMBER만 가능하다.
 * 타입별 필터링 등은 프론트가 전체 목록을 받아 클라이언트에서 처리하므로 쿼리 파라미터는 두지 않는다.
 */
@RestController
public class ArchiveItemController {

	private final ArchiveItemService archiveItemService;

	public ArchiveItemController(ArchiveItemService archiveItemService) {
		this.archiveItemService = archiveItemService;
	}

	@GetMapping("/api/projects/{projectId}/archive-items")
	public ResponseEntity<List<ArchiveItemResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId) {
		return ResponseEntity.ok(archiveItemService.listArchiveItems(principal, projectId));
	}

	@PostMapping("/api/projects/{projectId}/archive-items")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<ArchiveItemResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @Valid @RequestBody ArchiveItemCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(archiveItemService.createArchiveItem(principal, projectId, request));
	}
}
