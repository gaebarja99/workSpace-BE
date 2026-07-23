package com.teamsync.back.channel.dto;

import com.teamsync.back.channel.ChannelVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * FR-201 채널 생성 요청. visibility 미지정 시 서비스 계층에서 기본값(PUBLIC)을 적용한다.
 */
public record ChannelCreateRequest(
		@NotBlank(message = "채널 이름은 필수입니다.")
		@Size(max = 100)
		String name,

		ChannelVisibility visibility
) {
}
