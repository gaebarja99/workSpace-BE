package com.teamsync.back.report.dto;

import java.time.LocalDateTime;

/**
 * FR-401/402 "하이라이트" 섹션 1건. 개인 보고서가 아니라 project+weekStart~weekEnd 범위로 전체
 * 채널에서 하이라이트된 메시지를 모은 것이라 작성자와 무관하게 팀 전원의 보고서에 동일하게 노출된다.
 */
public record HighlightItem(
		Long messageId,
		Long channelId,
		String channelName,
		String authorName,
		String content,
		LocalDateTime createdAt
) {
}
