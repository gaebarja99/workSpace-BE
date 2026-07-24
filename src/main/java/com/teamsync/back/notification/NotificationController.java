package com.teamsync.back.notification;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.notification.dto.NotificationPreferencesResponse;
import com.teamsync.back.notification.dto.NotificationResponse;
import com.teamsync.back.notification.dto.UnreadCountResponse;
import com.teamsync.back.notification.dto.UpdateNotificationPreferencesRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-108(알림 트리거, US-04) & FR-003(알림 세분화 설정) API. 모든 엔드포인트는 인증된 사용자 본인의
 * 알림/설정만 다루므로 별도 @PreAuthorize 없이 인증 여부만 검증한다(SecurityConfig의 기본 인증 필터로 처리).
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;
	private final NotificationPreferenceService preferenceService;

	public NotificationController(NotificationService notificationService,
			NotificationPreferenceService preferenceService) {
		this.notificationService = notificationService;
		this.preferenceService = preferenceService;
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

	/**
	 * FR-003: 알림 세분화 설정 조회. 저장 여부와 무관하게 항상 5개 카테고리(기본값 보강)를 반환한다.
	 */
	@GetMapping("/preferences")
	public ResponseEntity<NotificationPreferencesResponse> getPreferences(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(preferenceService.getMyPreferences(principal));
	}

	/**
	 * FR-003: 알림 세분화 설정 부분 갱신(upsert). 본문에 포함된 카테고리만 갱신하고 전체 5개 카테고리를 반환한다.
	 */
	@PutMapping("/preferences")
	public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody UpdateNotificationPreferencesRequest request) {
		return ResponseEntity.ok(preferenceService.updateMyPreferences(principal, request));
	}
}
