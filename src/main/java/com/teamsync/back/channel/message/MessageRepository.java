package com.teamsync.back.channel.message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

	// 최상위 메시지와 스레드 답글을 구분하지 않고 하나의 평평한 리스트로 반환한다(프론트가 parentMessageId로 그룹핑).
	@EntityGraph(attributePaths = "author")
	List<Message> findAllByChannel_IdOrderByCreatedAtAscIdAsc(Long channelId);

	// parentMessageId 검증: 답글이 실제로 같은 채널에 속하는지 확인할 때 사용.
	Optional<Message> findByIdAndChannel_Id(Long id, Long channelId);

	// FR-004(통합 검색): content에 키워드가 포함된 메시지를 워크스페이스 범위로 조회하되, 사람이 작성하지
	// 않은 SYSTEM 메시지(FR-302)는 제외한다(excludedType으로 호출부에서 MessageType.SYSTEM을 넘긴다).
	// 응답 조립(channelName/projectName/authorName)에 필요한 연관을 모두 JOIN FETCH해 N+1을 피한다.
	// keyword는 호출부에서 LIKE 와일드카드(%, _)를 이스케이프해 넘겨야 한다(ESCAPE '\' 사용).
	@Query("SELECT m FROM Message m JOIN FETCH m.channel c JOIN FETCH c.project p LEFT JOIN FETCH m.author "
			+ "WHERE p.workspace.id = :workspaceId "
			+ "AND m.messageType <> :excludedType "
			+ "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' "
			+ "ORDER BY m.createdAt DESC, m.id DESC")
	List<Message> searchByWorkspace(@Param("workspaceId") Long workspaceId, @Param("keyword") String keyword,
			@Param("excludedType") MessageType excludedType, Pageable pageable);

	// FR-401/402(주간 보고 하이라이트 섹션): project+주차 범위로 하이라이트된 메시지를 실시간 조회한다.
	// (Message.highlighted=true AND 채널이 이 프로젝트 소속 AND createdAt이 [start, end) 범위, 계약 문서 기준.)
	// 응답 조립(channelName/authorName)에 필요한 연관을 JOIN FETCH해 N+1을 피한다.
	@Query("SELECT m FROM Message m JOIN FETCH m.channel c LEFT JOIN FETCH m.author "
			+ "WHERE c.project.id = :projectId AND m.highlighted = true AND m.createdAt >= :start AND m.createdAt < :end "
			+ "ORDER BY m.createdAt ASC, m.id ASC")
	List<Message> findHighlightsForProjectAndWeek(@Param("projectId") Long projectId,
			@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	// FR-410(보고 이력 검색): 해당 주차 범위에 keyword를 포함하는 하이라이트 메시지가 하나라도 있는지만 확인.
	// keyword는 호출부에서 LIKE 와일드카드(%, _)를 이스케이프해 넘겨야 한다(ESCAPE '\' 사용, FR-004와 동일 원칙).
	@Query("SELECT COUNT(m) > 0 FROM Message m JOIN m.channel c "
			+ "WHERE c.project.id = :projectId AND m.highlighted = true "
			+ "AND m.createdAt >= :start AND m.createdAt < :end "
			+ "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\'")
	boolean existsHighlightedContentMatch(@Param("projectId") Long projectId, @Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end, @Param("keyword") String keyword);
}
