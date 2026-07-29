package com.teamsync.back.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ProjectCreateRequest(
		@NotBlank(message = "프로젝트 이름은 필수입니다.")
		@Size(max = 200)
		String name,

		String description,

		LocalDate deadline,

		// 생성과 동시에 추가할 기존 워크스페이스 구성원 id 목록(선택). 생성자 본인은
		// 어차피 자동으로 첫 멤버가 되므로 여기 포함 여부와 무관하게 중복 추가되지 않는다.
		List<Long> memberIds
) {
}
