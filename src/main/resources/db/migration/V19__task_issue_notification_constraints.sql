-- FR-406(이슈/리스크 자동 플래그, 완전판) 후속 수정: V18에서 NotificationType.TASK_ISSUE_FLAGGED /
-- NotificationCategory.TASK_ISSUE를 추가했지만 이 두 CHECK 제약을 갱신하지 않아 배치가 알림을 저장하는
-- 순간 매번 예외로 롤백되고(chk_notifications_type), 사용자가 알림 설정을 저장할 때마다 500이 나는
-- (chk_notification_preferences_category, GET이 이제 6종을 내려주므로 기존 카테고리 저장도 함께 깨짐)
-- 결함이 QA 실 E2E에서 발견되어 이를 바로잡는다. V10/V11의 "DROP 후 재생성" 패턴을 그대로 따른다.

ALTER TABLE notifications DROP CONSTRAINT chk_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type
    CHECK (type IN ('TASK_DUE_SOON', 'TASK_DUE_TODAY', 'TASK_ASSIGNED', 'TASK_STATUS_CHANGED',
                     'WEEKLY_REPORT_REMINDER', 'MENTION', 'TASK_ISSUE_FLAGGED'));

ALTER TABLE notification_preferences DROP CONSTRAINT chk_notification_preferences_category;
ALTER TABLE notification_preferences ADD CONSTRAINT chk_notification_preferences_category
    CHECK (category IN ('TASK_DEADLINE', 'MENTION', 'TASK_ASSIGNED', 'TASK_STATUS_CHANGED',
                         'WEEKLY_REPORT', 'TASK_ISSUE'));
