package com.teamsync.back.task.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * FR-305(US-10) 태스크 댓글 작성 요청.
 * FR-105-A: mentionedUserIds는 optional(없으면 빈 배열로 취급). 워크스페이스 소속 유저만 유효하며 그 외는 무시된다.
 * content는 "@이름"을 포함한 원문 그대로 저장한다(별도 파싱/치환 없음).
 */
public record TaskCommentRequest(
		@NotBlank(message = "댓글 내용은 필수입니다.")
		String content,

		List<Long> mentionedUserIds
) {
}
