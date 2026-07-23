package com.teamsync.back.task;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskFileLinkRepository extends JpaRepository<TaskFileLink, Long> {

	// archivedFile과 그 업로더(uploadedBy 응답 필드)까지 함께 로딩해 목록 조회 시 N+1을 피한다.
	@EntityGraph(attributePaths = {"archivedFile", "archivedFile.uploader"})
	List<TaskFileLink> findByTaskId(Long taskId);

	boolean existsByTaskIdAndArchivedFileId(Long taskId, Long archivedFileId);

	void deleteByTaskIdAndArchivedFileId(Long taskId, Long archivedFileId);
}
