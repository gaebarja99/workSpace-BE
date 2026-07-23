package com.teamsync.back.channel;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.dto.ChannelCreateRequest;
import com.teamsync.back.channel.dto.ChannelResponse;
import com.teamsync.back.channel.dto.MessageCreateRequest;
import com.teamsync.back.channel.dto.MessageReactionSummary;
import com.teamsync.back.channel.dto.MessageResponse;
import com.teamsync.back.channel.dto.ReactionRequest;
import com.teamsync.back.channel.message.Message;
import com.teamsync.back.channel.message.MessageReaction;
import com.teamsync.back.channel.message.MessageReactionRepository;
import com.teamsync.back.channel.message.MessageRepository;
import com.teamsync.back.common.exception.ChannelNotFoundException;
import com.teamsync.back.common.exception.InvalidMessageRequestException;
import com.teamsync.back.common.exception.MessageNotFoundException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.notification.NotificationService;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-201(채널/토픽) / FR-202(실시간 메시징 — 이번 스코프는 REST + 프론트 폴링으로 근사, WebSocket은 후속 과제) 서비스.
 * TaskService/ProjectService와 동일한 원칙: 클라이언트가 전달한 projectId/channelId가 요청자의
 * 워크스페이스에 실제로 속하는지 항상 principal.workspaceId() 기준으로 재검증하고, 아니면 404로
 * 응답해 다른 워크스페이스 데이터의 존재 자체를 숨긴다(PRD 5.6 리스크 대응).
 */
@Service
public class ChannelService {

	private static final String DEFAULT_CHANNEL_NAME = "general";

	private final ChannelRepository channelRepository;
	private final MessageRepository messageRepository;
	private final MessageReactionRepository messageReactionRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;

	public ChannelService(ChannelRepository channelRepository, MessageRepository messageRepository,
			MessageReactionRepository messageReactionRepository, ProjectRepository projectRepository,
			UserRepository userRepository, NotificationService notificationService) {
		this.channelRepository = channelRepository;
		this.messageRepository = messageRepository;
		this.messageReactionRepository = messageReactionRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
	}

	/**
	 * 프로젝트에 채널이 하나도 없으면 이름 "general", 공개범위 PUBLIC인 기본 채널을 자동 생성해
	 * 반환에 포함시킨다(프론트가 빈 상태를 신경 쓸 필요 없게 하는 철학, currentProject.ts와 동일).
	 */
	@Transactional
	public List<ChannelResponse> listChannels(AuthenticatedUser principal, Long projectId) {
		Project project = getProjectInWorkspace(principal, projectId);

		List<Channel> channels = channelRepository.findAllByProject_IdOrderByIdAsc(projectId);
		if (channels.isEmpty()) {
			User createdBy = userRepository.getReferenceById(principal.userId());
			Channel defaultChannel = channelRepository.save(
					new Channel(project, DEFAULT_CHANNEL_NAME, ChannelVisibility.PUBLIC, createdBy));
			channels = List.of(defaultChannel);
		}

		return channels.stream().map(ChannelResponse::from).toList();
	}

	@Transactional
	public ChannelResponse createChannel(AuthenticatedUser principal, Long projectId, ChannelCreateRequest request) {
		Project project = getProjectInWorkspace(principal, projectId);
		User createdBy = userRepository.getReferenceById(principal.userId());

		Channel channel = channelRepository.save(new Channel(
				project,
				request.name().trim(),
				request.visibility() != null ? request.visibility() : ChannelVisibility.PUBLIC,
				createdBy));

		return ChannelResponse.from(channel);
	}

	@Transactional(readOnly = true)
	public List<MessageResponse> listMessages(AuthenticatedUser principal, Long channelId) {
		getChannelInWorkspace(principal, channelId);
		return messageRepository.findAllByChannel_IdOrderByCreatedAtAscIdAsc(channelId).stream()
				.map(message -> MessageResponse.from(message, principal.userId()))
				.toList();
	}

	/**
	 * FR-202-A(@멘션): 메시지 저장과 동일 트랜잭션에서 멘션 사용자(개별 + @전체)를 해석해 message_mentions에
	 * 기록하고, 본인 제외·중복 제거한 수신자에게 MENTION 알림(channel/message 딥링크)을 생성한다.
	 * mentionEveryone=true면 이 채널이 속한 프로젝트(=워크스페이스) 멤버 전원(본인 제외)이 알림 대상이 된다.
	 */
	@Transactional
	public MessageResponse createMessage(AuthenticatedUser principal, Long channelId, MessageCreateRequest request) {
		Channel channel = getChannelInWorkspace(principal, channelId);
		Message parentMessage = resolveParentMessage(channelId, request.parentMessageId());
		User author = userRepository.getReferenceById(principal.userId());

		Set<User> explicitMentions = resolveMentionedUsers(principal, request.mentionedUserIds());

		Message message = messageRepository.save(
				new Message(channel, author, request.content().trim(), parentMessage));
		message.applyMentions(explicitMentions, request.mentionEveryone());

		Set<User> recipients = new LinkedHashSet<>();
		if (request.mentionEveryone()) {
			recipients.addAll(userRepository.findAllByWorkspaceIdAndIdNotOrderByNameAsc(
					principal.workspaceId(), principal.userId()));
		}
		recipients.addAll(explicitMentions);
		notificationService.notifyMessageMentioned(message, recipients, principal.userId());

		return MessageResponse.from(message, principal.userId());
	}

	/**
	 * FR-202-B(이모지 반응 토글): 현재 유저가 해당 emoji 반응을 이미 보유하면 삭제, 없으면 추가한 뒤
	 * 해당 메시지의 갱신된 전체 반응 집계를 반환한다. UNIQUE(message_id, user_id, emoji) 제약과 함께
	 * "존재 여부 확인 → 추가/삭제"로 위반 없이 토글한다.
	 */
	@Transactional
	public List<MessageReactionSummary> toggleReaction(AuthenticatedUser principal, Long channelId, Long messageId,
			ReactionRequest request) {
		Message message = getMessageInChannel(principal, channelId, messageId);
		String emoji = request.emoji().trim();
		Long userId = principal.userId();

		messageReactionRepository.findByMessage_IdAndUser_IdAndEmoji(messageId, userId, emoji)
				.ifPresentOrElse(
						messageReactionRepository::delete,
						() -> messageReactionRepository.save(
								new MessageReaction(message, userRepository.getReferenceById(userId), emoji)));

		List<MessageReaction> reactions = messageReactionRepository.findByMessage_IdOrderByIdAsc(messageId);
		return MessageReactionSummary.summarize(reactions, userId);
	}

	/**
	 * FR-203(메시지 고정, US-07): ADMIN/LEADER만 호출 가능(컨트롤러 @PreAuthorize에서 제어).
	 * channelId가 요청자 워크스페이스 소속인지, messageId가 그 채널에 실제로 속하는지 모두 검증한다.
	 */
	@Transactional
	public MessageResponse pinMessage(AuthenticatedUser principal, Long channelId, Long messageId) {
		Message message = getMessageInChannel(principal, channelId, messageId);
		if (message.getParentMessage() != null) {
			throw new InvalidMessageRequestException("스레드 답글은 고정할 수 없습니다.");
		}
		message.pin();
		return MessageResponse.from(message, principal.userId());
	}

	/**
	 * unpinMessage는 답글 여부를 검증하지 않는다. pinMessage에서 답글 고정을 원천 차단했으므로
	 * 정상 흐름에서는 답글이 pinned=true가 될 수 없어 고아 상태 자체가 재발하지 않는다. 오히려
	 * 여기서도 답글을 막으면, 과거 데이터(마이그레이션/수동 조작 등)로 이미 고아 상태가 된 답글이
	 * 있을 때 API로 복구할 유일한 통로까지 막아버리므로 의도적으로 제한을 두지 않는다.
	 */
	@Transactional
	public MessageResponse unpinMessage(AuthenticatedUser principal, Long channelId, Long messageId) {
		Message message = getMessageInChannel(principal, channelId, messageId);
		message.unpin();
		return MessageResponse.from(message, principal.userId());
	}

	/**
	 * FR-402(주간 보고 하이라이트): pin과 달리 답글 여부를 검증하지 않는다(개인 큐레이션이라 스레드
	 * 답글도 하이라이트 대상이 될 수 있음). 컨트롤러의 @PreAuthorize가 ADMIN/LEADER/MEMBER(메시지
	 * 작성 권한과 동일)로 이미 제한하므로 서비스 계층에서 추가 역할 검증은 하지 않는다.
	 */
	@Transactional
	public MessageResponse highlightMessage(AuthenticatedUser principal, Long channelId, Long messageId) {
		Message message = getMessageInChannel(principal, channelId, messageId);
		message.highlight();
		return MessageResponse.from(message, principal.userId());
	}

	@Transactional
	public MessageResponse unhighlightMessage(AuthenticatedUser principal, Long channelId, Long messageId) {
		Message message = getMessageInChannel(principal, channelId, messageId);
		message.unhighlight();
		return MessageResponse.from(message, principal.userId());
	}

	/**
	 * FR-302: task.channelNotificationsEnabled가 켜져 있고 TaskMessageLink가 없는 태스크의 시스템 메시지
	 * 게시 대상을 찾을 때 사용(F2에서 구현된 프로젝트 기본 채널 "general" 조회 로직 재사용).
	 * general 채널이 아직 없으면(예: listChannels가 한 번도 호출되지 않은 신규 프로젝트) 빈 Optional을
	 * 반환하며, 호출자는 이 경우 조용히 건너뛰어야 한다(예외를 던지지 않음).
	 */
	@Transactional(readOnly = true)
	public Optional<Channel> findDefaultChannel(Long projectId) {
		return channelRepository.findFirstByProject_IdAndNameOrderByIdAsc(projectId, DEFAULT_CHANNEL_NAME);
	}

	/**
	 * FR-301(US-09): 메시지를 태스크로 전환할 때 TaskService가 channelId/messageId의 워크스페이스 소속
	 * 여부를 재검증하기 위해 재사용하는 조회. 다른 워크스페이스 데이터는 존재 자체가 노출되지 않도록
	 * 404로 응답한다(PRD 5.6 리스크 대응).
	 */
	@Transactional(readOnly = true)
	public Message getMessageInChannel(AuthenticatedUser principal, Long channelId, Long messageId) {
		getChannelInWorkspace(principal, channelId);
		return messageRepository.findByIdAndChannel_Id(messageId, channelId)
				.orElseThrow(MessageNotFoundException::new);
	}

	private Project getProjectInWorkspace(AuthenticatedUser principal, Long projectId) {
		return projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
	}

	private Channel getChannelInWorkspace(AuthenticatedUser principal, Long channelId) {
		return channelRepository.findByIdAndProject_Workspace_Id(channelId, principal.workspaceId())
				.orElseThrow(ChannelNotFoundException::new);
	}

	/**
	 * FR-202-A: 요청의 mentionedUserIds 중 요청자의 워크스페이스에 실제로 속한 사용자만 남긴다(그 외 id는 무시).
	 * null/빈 목록은 빈 Set으로 처리한다(태스크 담당자 해석과 달리, 존재하지 않는 id가 섞여도 예외 없이 무시).
	 */
	private Set<User> resolveMentionedUsers(AuthenticatedUser principal, Collection<Long> mentionedUserIds) {
		if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
			return new LinkedHashSet<>();
		}
		Set<Long> distinctIds = new LinkedHashSet<>(mentionedUserIds);
		distinctIds.remove(principal.userId()); // 작성자 자기 자신 멘션은 목록/알림 모두에서 제외
		return new LinkedHashSet<>(userRepository.findAllByIdInAndWorkspaceId(distinctIds, principal.workspaceId()));
	}

	private Message resolveParentMessage(Long channelId, Long parentMessageId) {
		if (parentMessageId == null) {
			return null;
		}
		return messageRepository.findByIdAndChannel_Id(parentMessageId, channelId)
				.orElseThrow(() -> new InvalidMessageRequestException("부모 메시지를 찾을 수 없습니다."));
	}
}
