package com.teamsync.back.channel;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.dto.ChannelCreateRequest;
import com.teamsync.back.channel.dto.ChannelResponse;
import com.teamsync.back.channel.dto.MessageCreateRequest;
import com.teamsync.back.channel.dto.MessageResponse;
import com.teamsync.back.channel.message.Message;
import com.teamsync.back.channel.message.MessageRepository;
import com.teamsync.back.common.exception.ChannelNotFoundException;
import com.teamsync.back.common.exception.InvalidMessageRequestException;
import com.teamsync.back.common.exception.MessageNotFoundException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.List;
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
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;

	public ChannelService(ChannelRepository channelRepository, MessageRepository messageRepository,
			ProjectRepository projectRepository, UserRepository userRepository) {
		this.channelRepository = channelRepository;
		this.messageRepository = messageRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
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
				.map(MessageResponse::from)
				.toList();
	}

	@Transactional
	public MessageResponse createMessage(AuthenticatedUser principal, Long channelId, MessageCreateRequest request) {
		Channel channel = getChannelInWorkspace(principal, channelId);
		Message parentMessage = resolveParentMessage(channelId, request.parentMessageId());
		User author = userRepository.getReferenceById(principal.userId());

		Message message = messageRepository.save(
				new Message(channel, author, request.content().trim(), parentMessage));

		return MessageResponse.from(message);
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
		return MessageResponse.from(message);
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
		return MessageResponse.from(message);
	}

	private Project getProjectInWorkspace(AuthenticatedUser principal, Long projectId) {
		return projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
	}

	private Channel getChannelInWorkspace(AuthenticatedUser principal, Long channelId) {
		return channelRepository.findByIdAndProject_Workspace_Id(channelId, principal.workspaceId())
				.orElseThrow(ChannelNotFoundException::new);
	}

	private Message getMessageInChannel(AuthenticatedUser principal, Long channelId, Long messageId) {
		getChannelInWorkspace(principal, channelId);
		return messageRepository.findByIdAndChannel_Id(messageId, channelId)
				.orElseThrow(MessageNotFoundException::new);
	}

	private Message resolveParentMessage(Long channelId, Long parentMessageId) {
		if (parentMessageId == null) {
			return null;
		}
		return messageRepository.findByIdAndChannel_Id(parentMessageId, channelId)
				.orElseThrow(() -> new InvalidMessageRequestException("부모 메시지를 찾을 수 없습니다."));
	}
}
