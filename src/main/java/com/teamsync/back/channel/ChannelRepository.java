package com.teamsync.back.channel;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

	List<Channel> findAllByProject_IdOrderByIdAsc(Long projectId);

	// 메시지 도메인(FR-202)에서 채널이 요청자의 워크스페이스 소속 프로젝트에 속하는지 스코핑 검증할 때 사용.
	// 다른 워크스페이스 채널은 조회되지 않아 존재 자체가 노출되지 않는다(PRD 5.6 리스크 대응).
	Optional<Channel> findByIdAndProject_Workspace_Id(Long id, Long workspaceId);

	// FR-302: TaskMessageLink가 없는 태스크의 시스템 메시지 게시 대상(프로젝트 기본 채널) 조회에 사용.
	Optional<Channel> findFirstByProject_IdAndNameOrderByIdAsc(Long projectId, String name);

	// 프로젝트 관리(관리자, P2) DELETE 사전 검증: 프로젝트에 채널이 하나라도 남아있는지 확인한다.
	boolean existsByProject_Id(Long projectId);
}
