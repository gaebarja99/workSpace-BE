package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-206: 요청한 DM 대화가 존재하지 않거나, 존재하더라도 호출자가 참가자가 아닌 경우.
 * 참가자가 아닌 경우에도 대화의 존재 자체를 숨기기 위해 동일하게 404로 응답한다(PRD 5.6 리스크 대응 원칙과 동일).
 */
public class DmConversationNotFoundException extends BusinessException {
	public DmConversationNotFoundException() {
		super(HttpStatus.NOT_FOUND, "DM_CONVERSATION_NOT_FOUND", "대화를 찾을 수 없습니다.");
	}
}
