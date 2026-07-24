package com.teamsync.back.invitation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

	Optional<Invitation> findByToken(String token);

	Optional<Invitation> findByIdAndWorkspaceId(Long id, Long workspaceId);

	Optional<Invitation> findByWorkspaceIdAndEmailAndStatus(Long workspaceId, String email, InvitationStatus status);

	List<Invitation> findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(Long workspaceId, InvitationStatus status);

	long countByWorkspaceIdAndStatus(Long workspaceId, InvitationStatus status);
}
