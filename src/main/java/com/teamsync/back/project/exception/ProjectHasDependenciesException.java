package com.teamsync.back.project.exception;

import com.teamsync.back.common.exception.BusinessException;

import org.springframework.http.HttpStatus;

/**
 * 프로젝트 관리(관리자, P2): DELETE /api/admin/projects/{id} 시 해당 프로젝트에 Task 등
 * 연관 데이터가 하나라도 남아있는 경우(V23부터 WeeklyReport는 project에 종속되지 않으므로 대상에서
 * 제외됨). projects를 참조하는 FK들이 ON DELETE 정책 미지정(RESTRICT)이므로 DB 제약 위반
 * (DataIntegrityViolationException) 대신 삭제 시도 전에 선제적으로 검증해 의미 있는 409로 응답한다.
 */
public class ProjectHasDependenciesException extends BusinessException {
	public ProjectHasDependenciesException() {
		super(HttpStatus.CONFLICT, "PROJECT_HAS_DEPENDENCIES", "연관된 태스크가 있어 삭제할 수 없습니다.");
	}
}
