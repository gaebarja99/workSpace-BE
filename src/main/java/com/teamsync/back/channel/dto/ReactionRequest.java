package com.teamsync.back.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * FR-202-B(메시지 이모지 반응) 토글 요청. 백엔드는 이모지 세트를 강제하지 않고 길이(≤ 16)만 검증한다.
 */
public record ReactionRequest(
		@NotBlank(message = "이모지는 필수입니다.")
		@Size(max = 16, message = "이모지는 16자를 넘을 수 없습니다.")
		String emoji
) {
}
