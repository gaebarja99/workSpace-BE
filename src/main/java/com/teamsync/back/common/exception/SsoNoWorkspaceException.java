package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-002 SSO: 이메일 도메인과 일치하는 워크스페이스가 없는 경우.
 * SSO는 워크스페이스명을 받을 수 없으므로 신규 워크스페이스 생성은 이메일 가입으로 유도한다.
 */
public class SsoNoWorkspaceException extends BusinessException {
	public SsoNoWorkspaceException(String domain) {
		super(HttpStatus.BAD_REQUEST, "SSO_NO_WORKSPACE",
				"'" + domain + "' 도메인의 워크스페이스가 없습니다. 이메일로 먼저 워크스페이스를 생성하세요.");
	}
}
