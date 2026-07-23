package com.teamsync.back.archive;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveItemRepository extends JpaRepository<ArchiveItem, Long> {

	// author(단일 연관)와 tags(컬렉션) 하나만 함께 즉시 로딩하므로 Task의 assignees+checklistItems
	// 조합에서 있었던 카티전 곱 중복 문제는 발생하지 않는다(컬렉션이 하나뿐).
	@EntityGraph(attributePaths = {"author", "tags"})
	List<ArchiveItem> findAllByProject_IdOrderByCreatedAtDescIdDesc(Long projectId);
}
