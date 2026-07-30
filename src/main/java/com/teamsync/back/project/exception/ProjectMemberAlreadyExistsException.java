package com.teamsync.back.project.exception;

import com.teamsync.back.common.exception.BusinessException;

import org.springframework.http.HttpStatus;

/** 프로젝트 멤버 관리: 이미 해당 프로젝트의 멤버인 사용자를 다시 추가하려는 경우. */
public class ProjectMemberAlreadyExistsException extends BusinessException {
	public ProjectMemberAlreadyExistsException() {
		super(HttpStatus.CONFLICT, "PROJECT_MEMBER_ALREADY_EXISTS", "이미 프로젝트 멤버로 등록된 사용자입니다.");
	}
}
