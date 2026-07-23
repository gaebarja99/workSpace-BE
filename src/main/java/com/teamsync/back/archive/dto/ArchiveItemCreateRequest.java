package com.teamsync.back.archive.dto;

import com.teamsync.back.archive.ArchiveItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * FR-205 정보 아카이브 항목 생성 요청. tags는 선택 항목이며 미지정/빈 값이면 빈 목록으로 저장된다.
 */
public record ArchiveItemCreateRequest(
		@NotNull(message = "유형은 필수입니다.")
		ArchiveItemType type,

		@NotBlank(message = "제목은 필수입니다.")
		@Size(max = 200)
		String title,

		@NotBlank(message = "내용은 필수입니다.")
		String content,

		List<String> tags
) {
}
