package com.teamsync.back.project.exception;

import com.teamsync.back.common.exception.BusinessException;

import org.springframework.http.HttpStatus;

/** 프로젝트 멤버 관리: 프로젝트 생성자는 멤버 목록에서 제거할 수 없다. */
public class RemoveProjectCreatorException extends BusinessException {
	public RemoveProjectCreatorException() {
		super(HttpStatus.BAD_REQUEST, "CANNOT_REMOVE_PROJECT_CREATOR", "프로젝트 생성자는 멤버에서 제거할 수 없습니다.");
	}
}
