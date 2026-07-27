package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.CategoryKeywordNotFoundException;
import com.teamsync.back.report.dto.CategoryKeywordCreateRequest;
import com.teamsync.back.report.dto.CategoryKeywordResponse;
import com.teamsync.back.report.dto.CategoryKeywordUpdateRequest;
import com.teamsync.back.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주간 보고(V23) 대/중분류 표준 코드 테이블 관리. 워크스페이스 내 인증된 사용자라면 누구나 추가/이름
 * 변경/비활성화할 수 있다(관리자 전용 아님) — 캡처 원본 엑셀처럼 팀원이 현장에서 즉석으로 새 대분류
 * (프로젝트)/중분류를 추가해야 하는 워크플로를 그대로 지원하기 위함이다.
 */
@Service
public class CategoryKeywordService {

	private final CategoryKeywordRepository categoryKeywordRepository;
	private final UserRepository userRepository;

	public CategoryKeywordService(CategoryKeywordRepository categoryKeywordRepository,
			UserRepository userRepository) {
		this.categoryKeywordRepository = categoryKeywordRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoryKeywordResponse> list(CategoryType type) {
		return categoryKeywordRepository.findAllByTypeAndActiveOrderByOrderIndexAscNameAsc(type, true).stream()
				.map(CategoryKeywordResponse::from)
				.toList();
	}

	/**
	 * 같은 type+name의 활성 항목이 이미 있으면 새로 만들지 않고 그대로 반환한다(계약: 프론트가 "새 항목
	 * 추가"를 안전하게 재시도할 수 있도록 409 대신 기존 항목 재사용). orderIndex는 기존 활성 항목 수
	 * 다음 순번으로 채번한다.
	 */
	@Transactional
	public CategoryKeywordResponse create(AuthenticatedUser principal, CategoryKeywordCreateRequest request) {
		String name = request.name().trim();
		CategoryKeyword existing = categoryKeywordRepository
				.findByTypeAndNameAndActive(request.type(), name, true).orElse(null);
		if (existing != null) {
			return CategoryKeywordResponse.from(existing);
		}
		int nextOrderIndex = categoryKeywordRepository
				.findAllByTypeAndActiveOrderByOrderIndexAscNameAsc(request.type(), true).size();
		CategoryKeyword keyword = categoryKeywordRepository.save(new CategoryKeyword(request.type(), name,
				nextOrderIndex, userRepository.getReferenceById(principal.userId())));
		return CategoryKeywordResponse.from(keyword);
	}

	@Transactional
	public CategoryKeywordResponse rename(Long id, CategoryKeywordUpdateRequest request) {
		CategoryKeyword keyword = categoryKeywordRepository.findById(id)
				.orElseThrow(CategoryKeywordNotFoundException::new);
		keyword.rename(request.name().trim());
		return CategoryKeywordResponse.from(keyword);
	}

	/** 물리 삭제 대신 active=false로 숨김 처리한다 — 과거 WeeklyReportEntry의 FK 참조는 그대로 유지된다. */
	@Transactional
	public void deactivate(Long id) {
		CategoryKeyword keyword = categoryKeywordRepository.findById(id)
				.orElseThrow(CategoryKeywordNotFoundException::new);
		keyword.deactivate();
	}
}
