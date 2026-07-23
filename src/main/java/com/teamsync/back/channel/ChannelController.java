package com.teamsync.back.channel;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.dto.ChannelCreateRequest;
import com.teamsync.back.channel.dto.ChannelResponse;
import com.teamsync.back.channel.dto.MessageCreateRequest;
import com.teamsync.back.channel.dto.MessageResponse;
import com.teamsync.back.task.TaskService;
import com.teamsync.back.task.dto.ConvertToTaskRequest;
import com.teamsync.back.task.dto.TaskResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-201(채널/토픽) / FR-202(실시간 메시징 — 이번 스코프는 REST + 프론트 폴링으로 근사, WebSocket은 후속 과제) /
 * FR-301(메시지→태스크 전환, US-09) API.
 * 조회(GET)는 인증된 워크스페이스 구성원이면 GUEST를 포함해 누구나 가능하고,
 * 생성(POST)은 TaskController/ProjectController와 동일하게 GUEST를 제외한 ADMIN/LEADER/MEMBER만 가능하다.
 * 태스크 전환(convert-to-task)은 Task 도메인 로직(TaskService)에 위임한다(엔드포인트 경로만 채널
 * 컨텍스트에 속함).
 */
@RestController
public class ChannelController {

	private final ChannelService channelService;
	private final TaskService taskService;

	public ChannelController(ChannelService channelService, TaskService taskService) {
		this.channelService = channelService;
		this.taskService = taskService;
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

	// FR-203(메시지 고정, US-07): "팀장으로서 공지를 고정"이므로 일반 MEMBER/GUEST는 고정할 수 없고
	// ADMIN/LEADER만 가능하다(메시지 작성은 MEMBER도 가능한 것과 의도적으로 다른 제약).
	@PostMapping("/api/channels/{channelId}/messages/{messageId}/pin")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<MessageResponse> pinMessage(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long channelId, @PathVariable Long messageId) {
		return ResponseEntity.ok(channelService.pinMessage(principal, channelId, messageId));
	}

	@DeleteMapping("/api/channels/{channelId}/messages/{messageId}/pin")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<MessageResponse> unpinMessage(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long channelId, @PathVariable Long messageId) {
		return ResponseEntity.ok(channelService.unpinMessage(principal, channelId, messageId));
	}

	// FR-301(메시지→태스크 전환, US-09): "팀원으로서" 즉시 전환하므로 메시지 작성과 동일하게
	// GUEST만 제외한 ADMIN/LEADER/MEMBER 전원 허용(핀보다 넓은 권한, 계약 문서 참고).
	@PostMapping("/api/channels/{channelId}/messages/{messageId}/convert-to-task")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<TaskResponse> convertToTask(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long channelId, @PathVariable Long messageId,
			@Valid @RequestBody ConvertToTaskRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(taskService.convertMessageToTask(principal, channelId, messageId, request));
	}
}
