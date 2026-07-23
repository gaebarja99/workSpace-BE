package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-304: 삭제(DELETE /api/tasks/{taskId}/files/{archivedFileId})하려는 파일 링크가 존재하지 않는 경우.
 */
public class TaskFileLinkNotFoundException extends BusinessException {
	public TaskFileLinkNotFoundException() {
		super(HttpStatus.NOT_FOUND, "TASK_FILE_LINK_NOT_FOUND", "연결된 파일을 찾을 수 없습니다.");
	}
}
