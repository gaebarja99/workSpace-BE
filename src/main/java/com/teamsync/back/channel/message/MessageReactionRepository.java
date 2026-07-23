package com.teamsync.back.channel.message;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

	// FR-202-B 토글: 현재 유저가 해당 메시지에 같은 이모지 반응을 이미 남겼는지 조회한다.
	Optional<MessageReaction> findByMessage_IdAndUser_IdAndEmoji(Long messageId, Long userId, String emoji);

	// 응답용 반응 집계: reactedByMe 판정을 위해 user를 함께 즉시 로딩한다(N+1 방지). id 오름차순으로
	// 이모지의 첫 등장 순서를 안정적으로 보존한다.
	@EntityGraph(attributePaths = "user")
	List<MessageReaction> findByMessage_IdOrderByIdAsc(Long messageId);
}
