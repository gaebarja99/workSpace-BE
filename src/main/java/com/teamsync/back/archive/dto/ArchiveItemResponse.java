package com.teamsync.back.archive.dto;

import com.teamsync.back.archive.ArchiveItem;
import com.teamsync.back.archive.ArchiveItemType;
import java.time.LocalDateTime;
import java.util.List;

public record ArchiveItemResponse(
		Long id,
		Long projectId,
		ArchiveItemType type,
		String title,
		String content,
		ArchiveAuthorResponse author,
		List<String> tags,
		LocalDateTime createdAt
) {
	public static ArchiveItemResponse from(ArchiveItem archiveItem) {
		return new ArchiveItemResponse(
				archiveItem.getId(),
				archiveItem.getProject().getId(),
				archiveItem.getType(),
				archiveItem.getTitle(),
				archiveItem.getContent(),
				archiveItem.getAuthor() != null ? ArchiveAuthorResponse.from(archiveItem.getAuthor()) : null,
				List.copyOf(archiveItem.getTags()),
				archiveItem.getCreatedAt());
	}
}
