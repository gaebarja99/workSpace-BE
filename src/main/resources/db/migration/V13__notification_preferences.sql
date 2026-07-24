-- FR-003 알림 세분화 설정: 사용자별 (카테고리 x 채널) 토글.
-- 카테고리(5종)마다 인앱/이메일/푸시 채널 on/off를 저장한다. 저장된 적 없는 카테고리는
-- 애플리케이션 기본값 매트릭스로 채워 응답하므로, 이 테이블에는 "사용자가 실제로 바꾼" 값만 upsert된다.
-- ddl-auto=validate이므로 이 스키마가 NotificationPreference 엔티티와 정확히 일치해야 한다.

CREATE TABLE notification_preferences (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category   VARCHAR(30) NOT NULL,
    in_app     BOOLEAN NOT NULL,
    email      BOOLEAN NOT NULL,
    push       BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    -- 사용자당 카테고리 1행. PUT 부분 upsert의 충돌 판정 기준.
    CONSTRAINT uk_notification_preferences_user_category UNIQUE (user_id, category),
    CONSTRAINT chk_notification_preferences_category
        CHECK (category IN ('TASK_DEADLINE', 'MENTION', 'TASK_ASSIGNED', 'TASK_STATUS_CHANGED', 'WEEKLY_REPORT'))
);

-- 발송 훅에서 "특정 카테고리 + 수신자 집합"을 배치로 조회할 때(N+1 방지) 사용.
CREATE INDEX idx_notification_preferences_category_user ON notification_preferences (category, user_id);
