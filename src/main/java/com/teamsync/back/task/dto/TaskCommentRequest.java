package com.teamsync.back.task.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * FR-305(US-10): 태스크 댓글 작성 요청.
 */
public record TaskCommentRequest(
		@NotBlank(message = "댓글 내용은 필수입니다.")
		String content
) {
}
