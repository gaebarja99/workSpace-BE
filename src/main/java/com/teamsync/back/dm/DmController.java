package com.teamsync.back.dm;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.dm.dto.DmContactResponse;
import com.teamsync.back.dm.dto.DmConversationCreateRequest;
import com.teamsync.back.dm.dto.DmConversationResponse;
import com.teamsync.back.dm.dto.DmConversationSummaryResponse;
import com.teamsync.back.dm.dto.DmMessageCreateRequest;
import com.teamsync.back.dm.dto.DmMessageResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-206(1:1 및 소그룹 다이렉트 메시지) API.
 * ChannelController/TaskController와 동일한 원칙: 조회(GET)는 인증된 사용자면 누구나 호출 가능하지만
 * 서비스 계층에서 호출자가 해당 대화의 참가자인지 확인해 아니면 404로 응답하고, 생성/전송(POST)은
 * GUEST를 제외한 ADMIN/LEADER/MEMBER만 가능하다.
 */
@RestController
@RequestMapping("/api/dm")
public class DmController {

	private final DmService dmService;

	public DmController(DmService dmService) {
		this.dmService = dmService;
	}

	@GetMapping("/contacts")
	public ResponseEntity<List<DmContactResponse>> listContacts(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(dmService.listContacts(principal));
	}

	@PostMapping("/conversations")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<DmConversationResponse> createConversation(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody DmConversationCreateRequest request) {
		DmService.CreateConversationResult result = dmService.createConversation(principal, request);
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
				.body(result.response());
	}

	@GetMapping("/conversations")
	public ResponseEntity<List<DmConversationSummaryResponse>> listConversations(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(dmService.listConversations(principal));
	}

	@GetMapping("/conversations/{conversationId}/messages")
	public ResponseEntity<List<DmMessageResponse>> listMessages(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long conversationId) {
		return ResponseEntity.ok(dmService.listMessages(principal, conversationId));
	}

	@PostMapping("/conversations/{conversationId}/messages")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<DmMessageResponse> sendMessage(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long conversationId, @Valid @RequestBody DmMessageCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(dmService.sendMessage(principal, conversationId, request));
	}
}
