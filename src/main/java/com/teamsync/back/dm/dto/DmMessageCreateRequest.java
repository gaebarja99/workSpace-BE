package com.teamsync.back.dm.dto;

import jakarta.validation.constraints.NotBlank;

public record DmMessageCreateRequest(
		@NotBlank(message = "메시지 내용은 필수입니다.")
		String content
) {
}
