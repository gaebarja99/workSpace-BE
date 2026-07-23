package com.teamsync.back.channel;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

	List<Channel> findAllByProject_IdOrderByIdAsc(Long projectId);

	// 메시지 도메인(FR-202)에서 채널이 요청자의 워크스페이스 소속 프로젝트에 속하는지 스코핑 검증할 때 사용.
	// 다른 워크스페이스 채널은 조회되지 않아 존재 자체가 노출되지 않는다(PRD 5.6 리스크 대응).
	Optional<Channel> findByIdAndProject_Workspace_Id(Long id, Long workspaceId);
}
