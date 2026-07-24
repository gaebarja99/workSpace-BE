package com.teamsync.back.notification.dto;

import com.teamsync.back.notification.EffectiveChannels;
import com.teamsync.back.notification.NotificationCategory;

/**
 * FR-003 GET/PUT 응답의 카테고리 1건. category는 enum 이름 문자열로 직렬화된다.
 */
public record NotificationPreferenceView(
		NotificationCategory category,
		boolean inApp,
		boolean email,
		boolean push
) {
	public static NotificationPreferenceView of(NotificationCategory category, EffectiveChannels channels) {
		return new NotificationPreferenceView(category, channels.inApp(), channels.email(), channels.push());
	}
}
