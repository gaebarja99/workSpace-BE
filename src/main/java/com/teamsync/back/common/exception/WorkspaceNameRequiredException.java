package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-000: 이메일 도메인과 일치하는 워크스페이스가 아직 없는 경우,
 * 신규 워크스페이스를 생성하려면 회사명(workspaceName)이 반드시 필요하다.
 */
public class WorkspaceNameRequiredException extends BusinessException {
	public WorkspaceNameRequiredException(String domain) {
		super(HttpStatus.BAD_REQUEST, "WORKSPACE_NAME_REQUIRED",
				"'" + domain + "' 도메인으로 등록된 워크스페이스가 없습니다. 새 워크스페이스를 만들려면 workspaceName을 입력하세요.");
	}
}
