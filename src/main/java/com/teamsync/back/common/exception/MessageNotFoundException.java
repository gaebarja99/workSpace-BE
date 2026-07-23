package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 메시지가 존재하지 않거나 다른 채널에 속해 요청자에게 보이지 않아야 하는 경우.
 */
public class MessageNotFoundException extends BusinessException {
	public MessageNotFoundException() {
		super(HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND", "메시지를 찾을 수 없습니다.");
	}
}
