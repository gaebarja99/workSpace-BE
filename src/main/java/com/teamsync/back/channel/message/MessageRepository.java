package com.teamsync.back.channel.message;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

	// 최상위 메시지와 스레드 답글을 구분하지 않고 하나의 평평한 리스트로 반환한다(프론트가 parentMessageId로 그룹핑).
	@EntityGraph(attributePaths = "author")
	List<Message> findAllByChannel_IdOrderByCreatedAtAscIdAsc(Long channelId);

	// parentMessageId 검증: 답글이 실제로 같은 채널에 속하는지 확인할 때 사용.
	Optional<Message> findByIdAndChannel_Id(Long id, Long channelId);
}
