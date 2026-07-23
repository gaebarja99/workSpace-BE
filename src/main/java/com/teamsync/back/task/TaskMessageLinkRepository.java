package com.teamsync.back.task;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskMessageLinkRepository extends JpaRepository<TaskMessageLink, Long> {

	// FR-303(관련 대화 보기)/FR-302(채널 결정 로직)/FR-305(댓글 동기화 대상 채널 결정)에서 공통으로 사용.
	// message.channel까지 함께 로딩해 TaskResponse 변환·시스템 메시지 게시 시 추가 쿼리 없이 channel.id에 접근한다.
	@EntityGraph(attributePaths = {"message", "message.channel"})
	Optional<TaskMessageLink> findByTaskId(Long taskId);

	// FR-301: 이미 태스크로 전환된 메시지인지 검증할 때 사용.
	boolean existsByMessage_Id(Long messageId);
}
