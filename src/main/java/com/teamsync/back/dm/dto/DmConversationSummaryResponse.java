package com.teamsync.back.dm.dto;

import com.teamsync.back.dm.DmConversation;
import com.teamsync.back.dm.DmMessage;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /api/dm/conversations 응답. participants는 호출자 본인을 제외한 나머지 참가자만 포함한다
 * (프론트에서 상대 이름 표시 용도). lastMessage는 메시지가 하나도 없으면 null이다.
 */
public record DmConversationSummaryResponse(
		Long id,
		List<DmParticipantResponse> participants,
		DmLastMessageResponse lastMessage,
		LocalDateTime createdAt
) {
	public static DmConversationSummaryResponse from(DmConversation conversation, Long callerId,
			DmMessage lastMessage) {
		List<DmParticipantResponse> others = conversation.getParticipants().stream()
				.filter(participant -> !participant.getId().equals(callerId))
				.map(DmParticipantResponse::from)
				.toList();
		return new DmConversationSummaryResponse(
				conversation.getId(),
				others,
				lastMessage != null ? DmLastMessageResponse.from(lastMessage) : null,
				conversation.getCreatedAt());
	}
}
