package com.teamsync.back.archive.file.dto;

import com.teamsync.back.archive.file.ArchivedFile;
import java.time.LocalDateTime;
import java.util.List;

public record ArchivedFileResponse(
		Long id,
		Long projectId,
		String filename,
		String contentType,
		long sizeBytes,
		UploaderResponse uploader,
		List<String> tags,
		LocalDateTime createdAt
) {
	public static ArchivedFileResponse from(ArchivedFile archivedFile) {
		return new ArchivedFileResponse(
				archivedFile.getId(),
				archivedFile.getProject().getId(),
				archivedFile.getOriginalFilename(),
				archivedFile.getContentType(),
				archivedFile.getSizeBytes(),
				UploaderResponse.from(archivedFile.getUploader()),
				List.copyOf(archivedFile.getTags()),
				archivedFile.getCreatedAt());
	}
}
