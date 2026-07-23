package com.teamsync.back.dm;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DmMessageRepository extends JpaRepository<DmMessage, Long> {

	// 페이지네이션 없이 대화 전체 메시지를 시간순으로 반환한다(채널 메시지 조회와 동일 컨벤션).
	@EntityGraph(attributePaths = "author")
	List<DmMessage> findAllByConversation_IdOrderByCreatedAtAscIdAsc(Long conversationId);

	// GET /api/dm/conversations의 lastMessage 조립용: 대화별 가장 최근 메시지 1건.
	@EntityGraph(attributePaths = "author")
	Optional<DmMessage> findFirstByConversation_IdOrderByCreatedAtDescIdDesc(Long conversationId);
}
