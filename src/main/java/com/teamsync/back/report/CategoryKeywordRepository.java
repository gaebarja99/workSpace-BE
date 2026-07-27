package com.teamsync.back.report;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryKeywordRepository extends JpaRepository<CategoryKeyword, Long> {

	// GET /api/category-keywords?type=: 활성 목록, orderIndex asc -> name asc.
	List<CategoryKeyword> findAllByTypeAndActiveOrderByOrderIndexAscNameAsc(CategoryType type, boolean active);

	// POST /api/category-keywords: 같은 type+name의 활성 항목이 이미 있으면 새로 만들지 않고 그대로 재사용한다
	// (프론트가 "새 항목 추가"를 안전하게 재시도할 수 있도록 409 대신 기존 항목을 반환).
	Optional<CategoryKeyword> findByTypeAndNameAndActive(CategoryType type, String name, boolean active);
}
