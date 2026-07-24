package com.teamsync.back.notification.dto;

import java.util.List;

/**
 * FR-003 GET/PUT /api/notifications/preferences 응답. 저장 여부와 무관하게 항상 5개 카테고리를
 * 계약의 노출 순서(NotificationCategory 선언 순)대로 담는다.
 */
public record NotificationPreferencesResponse(List<NotificationPreferenceView> categories) {
}
