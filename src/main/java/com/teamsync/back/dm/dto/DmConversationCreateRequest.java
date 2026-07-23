package com.teamsync.back.dm.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * POST /api/dm/conversations 요청. participantIds에는 본인을 포함하지 않아도 되며(서버가 자동으로
 * 추가), 실수로 포함되어도 서비스 계층에서 무해하게 제거한다. 참가자가 2명(본인 포함)이면 1:1로 취급해
 * 기존 대화를 재사용하고, 3명 이상이면 그룹으로 취급해 항상 신규 생성한다.
 */
public record DmConversationCreateRequest(
		@NotEmpty(message = "대화 상대는 최소 1명 이상 지정해야 합니다.")
		List<Long> participantIds
) {
}
