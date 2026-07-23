package com.teamsync.back.task.dto;

import jakarta.validation.constraints.NotNull;

/**
 * FR-304(US-09): 태스크에 아카이브 파일을 링크하는 요청. 파일 자체는 재업로드하지 않는다.
 */
public record TaskFileLinkRequest(
		@NotNull(message = "연결할 파일 ID는 필수입니다.")
		Long archivedFileId
) {
}
