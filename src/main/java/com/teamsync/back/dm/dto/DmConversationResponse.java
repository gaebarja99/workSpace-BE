package com.teamsync.back.dm.dto;

import com.teamsync.back.dm.DmConversation;
import java.time.LocalDateTime;
import java.util.List;

/**
 * POST /api/dm/conversations 응답. GET /api/dm/conversations(DmConversationSummaryResponse)와 달리
 * participants에 호출자 본인을 포함한 전체 참가자를 담는다(계약 문서 fr206-contract.md 참고).
 */
public record DmConversationResponse(
		Long id,
		List<DmParticipantResponse> participants,
		LocalDateTime createdAt
) {
	public static DmConversationResponse from(DmConversation conversation) {
		return new DmConversationResponse(
				conversation.getId(),
				conversation.getParticipants().stream().map(DmParticipantResponse::from).toList(),
				conversation.getCreatedAt());
	}
}
