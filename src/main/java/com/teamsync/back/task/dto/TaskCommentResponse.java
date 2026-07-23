package com.teamsync.back.task.dto;

import com.teamsync.back.task.TaskComment;
import com.teamsync.back.user.dto.MentionUser;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FR-305(US-10) 응답. FR-105-A: mentionedUsers는 이 댓글에서 언급된(워크스페이스 소속) 사용자 목록이다.
 */
public record TaskCommentResponse(
		Long id,
		String authorName,
		String content,
		List<MentionUser> mentionedUsers,
		LocalDateTime createdAt
) {
	public static TaskCommentResponse from(TaskComment comment) {
		return new TaskCommentResponse(
				comment.getId(),
				comment.getAuthor().getName(),
				comment.getContent(),
				comment.getMentionedUsers().stream().map(MentionUser::from).toList(),
				comment.getCreatedAt());
	}
}
