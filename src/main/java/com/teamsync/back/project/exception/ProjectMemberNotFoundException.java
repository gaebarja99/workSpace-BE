package com.teamsync.back.project.exception;

import com.teamsync.back.common.exception.BusinessException;

import org.springframework.http.HttpStatus;

/** 프로젝트 멤버 관리: 대상 사용자가 해당 프로젝트의 멤버가 아닌 경우(제거 시도 등). */
public class ProjectMemberNotFoundException extends BusinessException {
	public ProjectMemberNotFoundException() {
		super(HttpStatus.NOT_FOUND, "PROJECT_MEMBER_NOT_FOUND", "프로젝트 멤버를 찾을 수 없습니다.");
	}
}
