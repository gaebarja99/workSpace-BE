package com.teamsync.back.channel;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.dto.ChannelCreateRequest;
import com.teamsync.back.channel.dto.ChannelResponse;
import com.teamsync.back.channel.dto.MessageCreateRequest;
import com.teamsync.back.channel.dto.MessageResponse;
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
 * FR-201(채널/토픽) / FR-202(실시간 메시징 — 이번 스코프는 REST + 프론트 폴링으로 근사, WebSocket은 후속 과제) API.
 * 조회(GET)는 인증된 워크스페이스 구성원이면 GUEST를 포함해 누구나 가능하고,
 * 생성(POST)은 TaskController/ProjectController와 동일하게 GUEST를 제외한 ADMIN/LEADER/MEMBER만 가능하다.
 */
@RestController
public class ChannelController {

	private final ChannelService channelService;

	public ChannelController(ChannelService channelService) {
		this.channelService = channelService;
	}

	@GetMapping("/api/projects/{projectId}/channels")
	public ResponseEntity<List<ChannelResponse>> listChannels(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId) {
		return ResponseEntity.ok(channelService.listChannels(principal, projectId));
	}

	@PostMapping("/api/projects/{projectId}/channels")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<ChannelResponse> createChannel(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @Valid @RequestBody ChannelCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(channelService.createChannel(principal, projectId, request));
	}

	@GetMapping("/api/channels/{channelId}/messages")
	public ResponseEntity<List<MessageResponse>> listMessages(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long channelId) {
		return ResponseEntity.ok(channelService.listMessages(principal, channelId));
	}

	@PostMapping("/api/channels/{channelId}/messages")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<MessageResponse> createMessage(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long channelId, @Valid @RequestBody MessageCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(channelService.createMessage(principal, channelId, request));
	}
}
