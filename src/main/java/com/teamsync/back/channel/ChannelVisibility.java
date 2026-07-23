package com.teamsync.back.channel;

/**
 * FR-201 채널 공개 범위. 이번 범위에서는 PRIVATE에 대한 별도 접근 제어(멤버십 검증)는 구현하지 않고
 * 워크스페이스 소속 여부까지만 검증한다(PreAuthorize/워크스페이스 스코핑과 동일 원칙). 채널 단위
 * 세부 권한(PRIVATE 채널 멤버 제한)은 후속 과제다.
 */
public enum ChannelVisibility {
	PUBLIC,
	PRIVATE
}
