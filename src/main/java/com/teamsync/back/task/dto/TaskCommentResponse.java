package com.teamsync.back.task.dto;

import com.teamsync.back.task.TaskComment;
import java.time.LocalDateTime;

/**
 * FR-305(US-10) 응답.
 */
public record TaskCommentResponse(
		Long id,
		String authorName,
		String content,
		LocalDateTime createdAt
) {
	public static TaskCommentResponse from(TaskComment comment) {
		return new TaskCommentResponse(
				comment.getId(),
				comment.getAuthor().getName(),
				comment.getContent(),
				comment.getCreatedAt());
	}
}
