package com.teamsync.back.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	// GET /api/notifications/me: 최대 50건, createdAt DESC. 목록 응답에 taskId/projectId를
	// 채우기 위해 task와 task.project까지 함께 즉시 로딩한다(N+1 방지).
	@EntityGraph(attributePaths = {"task", "task.project"})
	List<Notification> findTop50ByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

	long countByRecipient_IdAndReadFalse(Long recipientId);

	@EntityGraph(attributePaths = {"task", "task.project"})
	Optional<Notification> findByIdAndRecipient_Id(Long id, Long recipientId);

	List<Notification> findAllByRecipient_IdAndReadFalse(Long recipientId);

	// FR-108 마감 임박 배치의 중복 방지: 같은 수신자·같은 태스크·같은 타입으로 오늘(00:00~24:00,
	// [start, end) 반개구간) 이미 생성된 알림이 있으면 재생성하지 않는다.
	boolean existsByRecipient_IdAndTask_IdAndTypeAndCreatedAtBetween(
			Long recipientId, Long taskId, NotificationType type, LocalDateTime createdAtStart,
			LocalDateTime createdAtEnd);

	// FR-408 자동 배치(WeeklyReportService.remindUnsubmittedReports)의 하루/한 주 중복 방지: 태스크와
	// 무관한 알림(WEEKLY_REPORT_REMINDER)이라 task_id 없이 recipient+type+기간으로만 멱등성을 검사한다.
	// 계약 문서 문면 그대로 "같은 주/같은 타입"만 기준으로 삼으며, 여러 프로젝트에 속한 사용자라도 이번 주에
	// 이미 한 번 리마인드를 받았다면 다른 프로젝트 몫은 다시 보내지 않는다(수동 재발송 버튼은 이 검사를 타지 않음).
	boolean existsByRecipient_IdAndTypeAndCreatedAtBetween(
			Long recipientId, NotificationType type, LocalDateTime createdAtStart, LocalDateTime createdAtEnd);
}
