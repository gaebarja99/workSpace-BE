package com.teamsync.back.notification.dto;

import java.util.List;

/**
 * FR-003 PUT /api/notifications/preferences 요청. 본문에 포함된 카테고리만 부분 upsert하며,
 * 미포함 카테고리는 기존/기본값을 유지한다.
 *
 * categories 누락(null)/비배열은 400, 알 수 없는 category 문자열도 400으로 처리한다(서비스에서 검증).
 * 각 채널 필드(inApp/email/push)가 누락되면 해당 카테고리의 기본값 매트릭스 값으로 간주한다(Boolean nullable).
 */
public record UpdateNotificationPreferencesRequest(List<Item> categories) {

	public record Item(
			String category,
			Boolean inApp,
			Boolean email,
			Boolean push
	) {
	}
}
