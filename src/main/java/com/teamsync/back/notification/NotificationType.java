package com.teamsync.back.notification;

/**
 * FR-108(알림 트리거, US-04)에서 발생 가능한 알림 종류.
 */
public enum NotificationType {
	TASK_DUE_SOON,
	TASK_DUE_TODAY,
	TASK_ASSIGNED,
	TASK_STATUS_CHANGED,
	// FR-408(주간 보고 미제출 리마인드): 팀장/관리자의 수동 재발송(POST /reports/team/remind, 멱등성 체크 없음)과
	// 매주 금요일 09:00 KST 자동 배치(WeeklyReportService.remindUnsubmittedReports, dedup 있음) 모두 이 타입을 쓴다.
	WEEKLY_REPORT_REMINDER,
	// FR-105-A(태스크 댓글 @멘션) / FR-202-A(메시지 @멘션): 누군가 댓글/메시지에서 회원을 언급하면 생성된다.
	// 태스크 댓글 멘션은 task 딥링크를, 메시지 멘션은 channel_id+message_id 딥링크를 함께 채운다.
	MENTION,
	// FR-406(이슈/리스크 자동 플래그, 완전판): 일 배치가 태스크에 OVERDUE/STALE/BLOCKED 플래그를 새로
	// 생성할 때, 담당자 전원 + 프로젝트 소속 워크스페이스의 LEADER/ADMIN에게 발송된다. task 딥링크를 채운다.
	TASK_ISSUE_FLAGGED
}
