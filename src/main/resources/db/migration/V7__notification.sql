-- FR-108 알림 트리거(US-04): 담당자 지정(TASK_ASSIGNED), 상태 변경(TASK_STATUS_CHANGED),
-- 마감 임박 D-1/D-0(TASK_DUE_SOON/TASK_DUE_TODAY) 발생 시 개인화 알림을 생성한다.
-- task_id는 알림 클릭 시 해당 태스크로 이동하는 딥링크 용도로만 참조한다. 태스크가 나중에
-- 삭제되더라도 알림 이력 자체는 보존해야 하므로 nullable + ON DELETE SET NULL로 구성해
-- tasks 삭제가 notifications 존재로 인해 막히지 않도록 한다.

CREATE TABLE notifications (
    id           BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users (id),
    type         VARCHAR(30) NOT NULL,
    message      VARCHAR(500) NOT NULL,
    task_id      BIGINT REFERENCES tasks (id) ON DELETE SET NULL,
    is_read      BOOLEAN NOT NULL DEFAULT false,
    read_at      TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_notifications_type
        CHECK (type IN ('TASK_DUE_SOON', 'TASK_DUE_TODAY', 'TASK_ASSIGNED', 'TASK_STATUS_CHANGED'))
);

-- GET /api/notifications/me: recipient_id 기준 createdAt DESC 상위 50건 조회 최적화
CREATE INDEX idx_notifications_recipient_id_created_at ON notifications (recipient_id, created_at DESC);

-- GET /api/notifications/me/unread-count, PATCH .../read-all: 미확인 알림 집계/일괄 처리 최적화
CREATE INDEX idx_notifications_recipient_id_is_read ON notifications (recipient_id, is_read);

-- 마감 임박 배치의 중복 방지 조회(recipient+task+type+오늘 생성분 존재 여부) 최적화
CREATE INDEX idx_notifications_task_id ON notifications (task_id);
