package com.teamsync.back.notification;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.notification.dto.NotificationResponse;
import com.teamsync.back.notification.dto.UnreadCountResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-108(알림 트리거, US-04) API. 모든 엔드포인트는 인증된 사용자 본인의 알림만 다루므로
 * 별도 @PreAuthorize 없이 인증 여부만 검증한다(SecurityConfig의 기본 인증 필터로 처리).
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping("/me")
	public ResponseEntity<List<NotificationResponse>> listMyNotifications(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(notificationService.listMyNotifications(principal));
	}

	@GetMapping("/me/unread-count")
	public ResponseEntity<UnreadCountResponse> unreadCount(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(new UnreadCountResponse(notificationService.countUnread(principal)));
	}

	@PatchMapping("/{id}/read")
	public ResponseEntity<NotificationResponse> markAsRead(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long id) {
		return ResponseEntity.ok(notificationService.markAsRead(principal, id));
	}

	@PostMapping("/me/read-all")
	public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal AuthenticatedUser principal) {
		notificationService.markAllAsRead(principal);
		return ResponseEntity.noContent().build();
	}
}
