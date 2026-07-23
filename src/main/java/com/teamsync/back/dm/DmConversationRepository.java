package com.teamsync.back.dm;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DmConversationRepository extends JpaRepository<DmConversation, Long> {

	// 1:1 대화 재사용(멱등) 판별: 참가자가 정확히 이 두 사람뿐인 기존 대화를 찾는다. 그룹(3명 이상)은
	// SIZE(c.participants) = 2 조건에 걸려 대상이 되지 않는다.
	@Query("SELECT c FROM DmConversation c JOIN c.participants p1 JOIN c.participants p2 "
			+ "WHERE p1.id = :userId1 AND p2.id = :userId2 AND SIZE(c.participants) = 2")
	Optional<DmConversation> findExistingDirectConversation(@Param("userId1") Long userId1,
			@Param("userId2") Long userId2);

	// GET /api/dm/conversations: 호출자가 참가자인 모든 대화(1:1 + 그룹)를 조회한다.
	@EntityGraph(attributePaths = "participants")
	@Query("SELECT DISTINCT c FROM DmConversation c JOIN c.participants p WHERE p.id = :userId")
	List<DmConversation> findAllByParticipantId(@Param("userId") Long userId);

	// 메시지 조회/전송 시 호출자가 해당 대화의 참가자인지 확인하기 위해 participants까지 함께 로딩한다.
	@EntityGraph(attributePaths = "participants")
	Optional<DmConversation> findWithParticipantsById(Long id);
}
