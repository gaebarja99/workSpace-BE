package com.teamsync.back.notification;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.Channel;
import com.teamsync.back.channel.message.Message;
import com.teamsync.back.common.exception.NotificationNotFoundException;
import com.teamsync.back.notification.dto.NotificationResponse;
import com.teamsync.back.project.Project;
import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.TaskStatus;
import com.teamsync.back.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-108(알림 트리거, US-04): 담당자 지정/상태 변경/마감 임박(D-1, D-0) 알림의 생성·조회·읽음 처리.
 * 생성 트리거(담당자 지정, 상태 변경)는 TaskService가 태스크 변경 트랜잭션 내에서 이 서비스를 호출해
 * 정형 데이터(태스크) 변경과 알림 발생이 같은 트랜잭션 경계 안에서 함께 커밋/롤백되도록 한다.
 * 마감 임박 알림만 별도의 일일 배치(@Scheduled)로 발생시킨다.
 */
@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final TaskRepository taskRepository;

	public NotificationService(NotificationRepository notificationRepository, TaskRepository taskRepository) {
		this.notificationRepository = notificationRepository;
		this.taskRepository = taskRepository;
	}

	@Transactional(readOnly = true)
	public List<NotificationResponse> listMyNotifications(AuthenticatedUser principal) {
		return notificationRepository.findTop50ByRecipient_IdOrderByCreatedAtDesc(principal.userId()).stream()
				.map(NotificationResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public long countUnread(AuthenticatedUser principal) {
		return notificationRepository.countByRecipient_IdAndReadFalse(principal.userId());
	}

	@Transactional
	public NotificationResponse markAsRead(AuthenticatedUser principal, Long notificationId) {
		Notification notification = notificationRepository
				.findByIdAndRecipient_Id(notificationId, principal.userId())
				.orElseThrow(NotificationNotFoundException::new);
		notification.markAsRead();
		return NotificationResponse.from(notification);
	}

	@Transactional
	public void markAllAsRead(AuthenticatedUser principal) {
		notificationRepository.findAllByRecipient_IdAndReadFalse(principal.userId())
				.forEach(Notification::markAsRead);
	}

	/**
	 * FR-108 트리거 1/2 (TASK_ASSIGNED): recipients 중 actingUserId(변경을 수행한 본인)는 제외하고
	 * 나머지 전원에게 담당자 지정 알림을 생성한다. 태스크 생성 시에는 초기 assignees 전체를,
	 * 태스크 수정 시에는 이전 담당자 집합에 없던 "신규 추가분"만 호출자가 골라 넘겨야 한다.
	 */
	@Transactional
	public void notifyTaskAssigned(Task task, Collection<User> recipients, Long actingUserId) {
		String message = "\"" + task.getTitle() + "\" 태스크가 회원님에게 할당되었습니다.";
		for (User recipient : recipients) {
			if (recipient.getId().equals(actingUserId)) {
				continue;
			}
			notificationRepository.save(new Notification(recipient, NotificationType.TASK_ASSIGNED, message, task));
		}
	}

	/**
	 * FR-108 트리거 3 (TASK_STATUS_CHANGED): 상태가 실제로 바뀐 경우에만 호출되어야 하며,
	 * 변경 시점의 담당자 전원(변경을 수행한 본인 제외)에게 알림을 생성한다.
	 */
	@Transactional
	public void notifyTaskStatusChanged(Task task, TaskStatus previousStatus, TaskStatus newStatus,
			Long actingUserId) {
		String message = "\"" + task.getTitle() + "\" 태스크 상태가 " + TaskStatusLabels.of(previousStatus) + "에서 "
				+ TaskStatusLabels.of(newStatus) + "(으)로 변경되었습니다.";
		for (User assignee : task.getAssignees()) {
			if (assignee.getId().equals(actingUserId)) {
				continue;
			}
			notificationRepository.save(new Notification(assignee, NotificationType.TASK_STATUS_CHANGED, message, task));
		}
	}

	/**
	 * FR-105-A(태스크 댓글 @멘션): 댓글에서 언급된 수신자 전원에게 MENTION 알림을 생성한다. 본인(actingUserId)은
	 * 제외하고, 같은 수신자가 중복 전달되어도 1건만 만든다. 딥링크는 task(댓글이 달린 태스크)로 건다.
	 * 호출자(TaskService.createTaskComment)가 이미 워크스페이스 소속만 걸러 recipients로 넘긴다.
	 */
	@Transactional
	public void notifyTaskCommentMentioned(Task task, User actor, Collection<User> recipients, String content,
			Long actingUserId) {
		String actorName = actor != null ? actor.getName() : "시스템";
		String message = actorName + "님이 회원님을 언급했습니다: " + snippet(content);
		Set<Long> notified = new HashSet<>();
		for (User recipient : recipients) {
			if (recipient.getId().equals(actingUserId) || !notified.add(recipient.getId())) {
				continue;
			}
			notificationRepository.save(Notification.forTaskMention(recipient, message, task));
		}
	}

	/**
	 * FR-202-A(메시지 @멘션): 메시지에서 언급된 수신자(개별 멘션 + @전체 대상)에게 MENTION 알림을 생성한다.
	 * 본인(actingUserId) 제외, 수신자 중복 제거. 딥링크는 channel_id + message_id로 건다.
	 */
	@Transactional
	public void notifyMessageMentioned(Message message, Collection<User> recipients, Long actingUserId) {
		String actorName = message.getAuthor() != null ? message.getAuthor().getName() : "시스템";
		String notificationMessage = actorName + "님이 회원님을 언급했습니다: " + snippet(message.getContent());
		Channel channel = message.getChannel();
		Set<Long> notified = new HashSet<>();
		for (User recipient : recipients) {
			if (recipient.getId().equals(actingUserId) || !notified.add(recipient.getId())) {
				continue;
			}
			notificationRepository.save(
					Notification.forMessageMention(recipient, notificationMessage, channel, message));
		}
	}

	/** 멘션 알림 문구용: 내용 앞 40자만 남기고, 잘렸으면 말줄임표를 붙인다. */
	private static String snippet(String content) {
		String trimmed = content == null ? "" : content.trim();
		if (trimmed.length() <= 40) {
			return trimmed;
		}
		return trimmed.substring(0, 40) + "…";
	}

	/**
	 * FR-108 트리거 4 (TASK_DUE_SOON=D-1, TASK_DUE_TODAY=D-0): 매일 09:00(KST)에 실행되는 배치.
	 * dueDate가 내일/오늘이고 DONE이 아닌 태스크의 담당자 전원에게 알림을 생성하되, 같은
	 * 수신자·태스크·타입 조합으로 오늘 이미 생성된 알림이 있으면 다시 만들지 않는다.
	 */
	@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
	@Transactional
	public void notifyUpcomingDueDates() {
		LocalDate today = LocalDate.now();
		createDueDateNotifications(today.plusDays(1), NotificationType.TASK_DUE_SOON, "내일");
		createDueDateNotifications(today, NotificationType.TASK_DUE_TODAY, "오늘");
	}

	private void createDueDateNotifications(LocalDate dueDate, NotificationType type, String dDayLabel) {
		LocalDateTime todayStart = LocalDate.now().atStartOfDay();
		LocalDateTime todayEnd = todayStart.plusDays(1);

		List<Task> dueTasks = taskRepository.findAllByDueDateAndStatusNot(dueDate, TaskStatus.DONE);
		for (Task task : dueTasks) {
			String message = "\"" + task.getTitle() + "\" 태스크 마감일이 " + dDayLabel + "입니다.";
			for (User assignee : task.getAssignees()) {
				boolean alreadyNotified = notificationRepository
						.existsByRecipient_IdAndTask_IdAndTypeAndCreatedAtBetween(
								assignee.getId(), task.getId(), type, todayStart, todayEnd);
				if (alreadyNotified) {
					continue;
				}
				notificationRepository.save(new Notification(assignee, type, message, task));
			}
		}
	}

	/**
	 * FR-408(주간 보고 미제출 리마인드, 수동 재발송): "리마인드 재발송" 버튼 호출 시 사용. 멱등성 체크를
	 * 전혀 하지 않고 호출될 때마다 즉시 알림을 생성한다(POST /reports/team/remind는 매번 재발송이 목적).
	 * task 딥링크가 없는 알림이므로 task는 항상 null이다.
	 */
	@Transactional
	public void notifyWeeklyReportReminder(Project project, User recipient) {
		String message = "\"" + project.getName() + "\" 프로젝트의 이번 주 주간 보고서를 아직 제출하지 않았습니다.";
		notificationRepository.save(new Notification(recipient, NotificationType.WEEKLY_REPORT_REMINDER, message, null));
	}

	/**
	 * FR-408 자동 배치 전용: recipient가 [weekRangeStart, weekRangeEnd) 기간 동안 이미
	 * WEEKLY_REPORT_REMINDER 알림을 받았으면 스킵하고, 아니면 생성한다. 반환값은 실제로 알림이
	 * 생성되었는지 여부(호출자의 카운트 집계 용도는 아니며, 배치 로그 목적).
	 */
	@Transactional
	public boolean notifyWeeklyReportReminderIfNeeded(Project project, User recipient, LocalDateTime weekRangeStart,
			LocalDateTime weekRangeEndExclusive) {
		boolean alreadyNotified = notificationRepository.existsByRecipient_IdAndTypeAndCreatedAtBetween(
				recipient.getId(), NotificationType.WEEKLY_REPORT_REMINDER, weekRangeStart, weekRangeEndExclusive);
		if (alreadyNotified) {
			return false;
		}
		notifyWeeklyReportReminder(project, recipient);
		return true;
	}
}
