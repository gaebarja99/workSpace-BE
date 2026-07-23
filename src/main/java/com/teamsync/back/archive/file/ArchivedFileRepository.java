package com.teamsync.back.archive.file;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArchivedFileRepository extends JpaRepository<ArchivedFile, Long> {

	// uploader(단일 연관)와 tags(컬렉션) 하나만 함께 즉시 로딩하므로 Task의 assignees+checklistItems
	// 조합에서 있었던 카티전 곱 중복 문제는 발생하지 않는다(컬렉션이 하나뿐).
	@EntityGraph(attributePaths = {"uploader", "tags"})
	List<ArchivedFile> findAllByProject_IdOrderByCreatedAtDescIdDesc(Long projectId);

	// 다운로드(FR-204)에서 fileId가 요청자 워크스페이스 소속 프로젝트에 속하는지 스코핑 검증할 때 사용.
	// 다른 워크스페이스 파일은 조회되지 않아 존재 자체가 노출되지 않는다(PRD 5.6 리스크 대응).
	@EntityGraph(attributePaths = "project")
	Optional<ArchivedFile> findByIdAndProject_Workspace_Id(Long id, Long workspaceId);

	// FR-004(통합 검색): originalFilename에 키워드가 포함된 파일을 워크스페이스 범위로 조회한다.
	// 응답 조립(projectName/uploadedBy)에 필요한 연관을 JOIN FETCH해 N+1을 피한다.
	// keyword는 호출부에서 LIKE 와일드카드(%, _)를 이스케이프해 넘겨야 한다(ESCAPE '\' 사용).
	@Query("SELECT f FROM ArchivedFile f JOIN FETCH f.project p JOIN FETCH f.uploader "
			+ "WHERE p.workspace.id = :workspaceId "
			+ "AND LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' "
			+ "ORDER BY f.createdAt DESC, f.id DESC")
	List<ArchivedFile> searchByWorkspace(@Param("workspaceId") Long workspaceId, @Param("keyword") String keyword,
			Pageable pageable);
}
