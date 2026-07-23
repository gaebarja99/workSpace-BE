package com.teamsync.back.task.dto;

import com.teamsync.back.archive.file.ArchivedFile;
import com.teamsync.back.task.TaskFileLink;

/**
 * FR-304(US-09) 응답. downloadUrl은 기존 아카이브 다운로드 엔드포인트
 * (GET /api/files/{fileId}/download, ArchivedFileController 참고)를 그대로 재사용한다.
 * uploadedBy는 태스크에 링크한 사람이 아니라 원본 파일을 아카이브에 업로드한 사람의 이름이다.
 */
public record TaskFileLinkResponse(
		Long id,
		Long archivedFileId,
		String originalFilename,
		String contentType,
		long sizeBytes,
		String uploadedBy,
		String downloadUrl
) {
	public static TaskFileLinkResponse from(TaskFileLink link) {
		ArchivedFile file = link.getArchivedFile();
		return new TaskFileLinkResponse(
				link.getId(),
				file.getId(),
				file.getOriginalFilename(),
				file.getContentType(),
				file.getSizeBytes(),
				file.getUploader().getName(),
				"/api/files/" + file.getId() + "/download");
	}
}
