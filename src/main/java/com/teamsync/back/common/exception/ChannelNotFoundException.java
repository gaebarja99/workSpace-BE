package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 채널이 존재하지 않거나 다른 워크스페이스 소속 프로젝트에 속해 요청자에게 보이지 않아야 하는 경우.
 */
public class ChannelNotFoundException extends BusinessException {
	public ChannelNotFoundException() {
		super(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND", "채널을 찾을 수 없습니다.");
	}
}
