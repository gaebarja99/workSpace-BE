package com.teamsync.back.member.exception;

import com.teamsync.back.common.exception.BusinessException;

import org.springframework.http.HttpStatus;

/** 프로젝트 멤버 관리: 프로젝트의 유일한 멤버는 제거할 수 없다(빈 프로젝트 방지). */
public class LastProjectMemberException extends BusinessException {
	public LastProjectMemberException() {
		super(HttpStatus.BAD_REQUEST, "LAST_PROJECT_MEMBER", "프로젝트의 마지막 멤버는 제거할 수 없습니다.");
	}
}
