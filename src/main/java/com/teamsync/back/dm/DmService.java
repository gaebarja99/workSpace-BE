package com.teamsync.back.dm;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.DmConversationNotFoundException;
import com.teamsync.back.common.exception.InvalidDmRequestException;
import com.teamsync.back.dm.dto.DmContactResponse;
import com.teamsync.back.dm.dto.DmConversationCreateRequest;
import com.teamsync.back.dm.dto.DmConversationResponse;
import com.teamsync.back.dm.dto.DmConversationSummaryResponse;
import com.teamsync.back.dm.dto.DmMessageCreateRequest;
import com.teamsync.back.dm.dto.DmMessageResponse;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-206(1:1 및 소그룹 다이렉트 메시지) 서비스.
 * TaskService/ChannelService와 동일한 원칙: 참가자 검증은 UserRepository.findAllByIdInAndWorkspaceId로
 * 호출자와 같은 워크스페이스인지 재확인하고, 조회 계열(GET) 메서드는 호출자가 해당 대화의 참가자가
 * 아니면 대화의 존재 자체를 숨기기 위해 404(DmConversationNotFoundException)로 응답한다(PRD 5.6 리스크 대응).
 */
@Service
public class DmService {

	private final DmConversationRepository conversationRepository;
	private final DmMessageRepository messageRepository;
	private final UserRepository userRepository;

	public DmService(DmConversationRepository conversationRepository, DmMessageRepository messageRepository,
			UserRepository userRepository) {
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<DmContactResponse> listContacts(AuthenticatedUser principal) {
		return userRepository
				.findAllByWorkspaceIdAndIdNotOrderByNameAsc(principal.workspaceId(), principal.userId())
				.stream()
				.map(DmContactResponse::from)
				.toList();
	}

	/**
	 * 1:1(참가자 2명, 본인 포함)은 정확히 같은 두 사람으로 구성된 기존 대화가 있으면 재사용(멱등)하고,
	 * 그룹(3명 이상)은 참가자 집합이 같아도 매번 새 대화를 생성한다(Slack 그룹DM과 동일 취급, 계약 문서 참고).
	 */
	@Transactional
	public CreateConversationResult createConversation(AuthenticatedUser principal,
			DmConversationCreateRequest request) {
		Set<Long> distinctOtherIds = new LinkedHashSet<>(request.participantIds());
		// 호출자가 실수로 자신을 participantIds에 포함해도 무해하게 제거한다(계약: participantIds에는
		// 본인을 포함하지 않음이 원칙이지만, 방어적으로 처리한다).
		distinctOtherIds.remove(principal.userId());
		if (distinctOtherIds.isEmpty()) {
			throw new InvalidDmRequestException("대화 상대는 최소 1명 이상 지정해야 합니다.");
		}

		List<User> others = userRepository.findAllByIdInAndWorkspaceId(distinctOtherIds, principal.workspaceId());
		if (others.size() != distinctOtherIds.size()) {
			throw new InvalidDmRequestException("유효하지 않은 대화 상대가 포함되어 있습니다.");
		}

		if (others.size() == 1) {
			Long otherUserId = others.get(0).getId();
			var existing = conversationRepository.findExistingDirectConversation(principal.userId(), otherUserId);
			if (existing.isPresent()) {
				return new CreateConversationResult(DmConversationResponse.from(existing.get()), false);
			}
		}

		User caller = userRepository.getReferenceById(principal.userId());
		Set<User> participants = new LinkedHashSet<>();
		participants.add(caller);
		participants.addAll(others);

		DmConversation saved = conversationRepository.save(new DmConversation(participants));
		return new CreateConversationResult(DmConversationResponse.from(saved), true);
	}

	@Transactional(readOnly = true)
	public List<DmConversationSummaryResponse> listConversations(AuthenticatedUser principal) {
		List<DmConversation> conversations = conversationRepository.findAllByParticipantId(principal.userId());

		record ConversationWithLastMessage(DmConversation conversation, DmMessage lastMessage) {
		}

		return conversations.stream()
				.map(conversation -> new ConversationWithLastMessage(conversation,
						messageRepository.findFirstByConversation_IdOrderByCreatedAtDescIdDesc(conversation.getId())
								.orElse(null)))
				.sorted(Comparator.comparing(
						(ConversationWithLastMessage cwm) -> cwm.lastMessage() != null
								? cwm.lastMessage().getCreatedAt()
								: cwm.conversation().getCreatedAt())
						.reversed())
				.map(cwm -> DmConversationSummaryResponse.from(cwm.conversation(), principal.userId(),
						cwm.lastMessage()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DmMessageResponse> listMessages(AuthenticatedUser principal, Long conversationId) {
		getConversationForParticipant(principal, conversationId);
		return messageRepository.findAllByConversation_IdOrderByCreatedAtAscIdAsc(conversationId).stream()
				.map(DmMessageResponse::from)
				.toList();
	}

	@Transactional
	public DmMessageResponse sendMessage(AuthenticatedUser principal, Long conversationId,
			DmMessageCreateRequest request) {
		DmConversation conversation = getConversationForParticipant(principal, conversationId);
		User author = userRepository.getReferenceById(principal.userId());

		DmMessage saved = messageRepository.save(new DmMessage(conversation, author, request.content().trim()));
		return DmMessageResponse.from(saved);
	}

	/**
	 * 호출자가 해당 대화의 참가자인지 확인한다. 대화가 없거나 참가자가 아니면 동일하게 404를 던져
	 * 다른 사용자의 DM 대화가 존재하는지 여부 자체가 노출되지 않도록 한다(PRD 5.6 리스크 대응).
	 * 모든 참가자는 대화 생성 시점에 이미 같은 워크스페이스 소속임이 검증되었으므로(createConversation),
	 * 이 확인만으로 워크스페이스 격리까지 함께 보장된다.
	 */
	private DmConversation getConversationForParticipant(AuthenticatedUser principal, Long conversationId) {
		DmConversation conversation = conversationRepository.findWithParticipantsById(conversationId)
				.orElseThrow(DmConversationNotFoundException::new);
		boolean isParticipant = conversation.getParticipants().stream()
				.anyMatch(participant -> participant.getId().equals(principal.userId()));
		if (!isParticipant) {
			throw new DmConversationNotFoundException();
		}
		return conversation;
	}

	public record CreateConversationResult(DmConversationResponse response, boolean created) {
	}
}
