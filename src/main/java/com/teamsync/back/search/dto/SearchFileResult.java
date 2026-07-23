package com.teamsync.back.search.dto;

import com.teamsync.back.archive.file.ArchivedFile;
import java.time.LocalDateTime;

/**
 * FR-004(통합 검색) 파일 검색 결과 항목(FR-204 파일 아카이브 대상, originalFilename 검색 매칭).
 * downloadUrl은 ArchivedFileController의 실제 다운로드 엔드포인트(GET /api/files/{fileId}/download)를 그대로 가리킨다.
 */
public record SearchFileResult(
		Long id,
		Long projectId,
		String projectName,
		String originalFilename,
		String contentType,
		long sizeBytes,
		String downloadUrl,
		String uploadedBy,
		LocalDateTime createdAt
) {
	public static SearchFileResult from(ArchivedFile archivedFile) {
		return new SearchFileResult(
				archivedFile.getId(),
				archivedFile.getProject().getId(),
				archivedFile.getProject().getName(),
				archivedFile.getOriginalFilename(),
				archivedFile.getContentType(),
				archivedFile.getSizeBytes(),
				"/api/files/" + archivedFile.getId() + "/download",
				archivedFile.getUploader().getName(),
				archivedFile.getCreatedAt());
	}
}
