-- FR-101 보드(프로젝트별 태스크 목록), FR-102 태스크 카드(담당자/일정/우선순위/상태/체크리스트)
-- 라벨/첨부파일/댓글·활동로그(FR-105)/반복 태스크(FR-106)/의존관계(FR-107)/대시보드(FR-104)/
-- 알림 트리거(FR-108)는 이번 마이그레이션 범위 밖이며 후속 마이그레이션(V4 이후)에서 확장한다.

CREATE TABLE tasks (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL REFERENCES projects (id),
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority     VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    start_date   DATE,
    due_date     DATE,
    created_by   BIGINT REFERENCES users (id),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'REVIEW', 'DONE')),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('URGENT', 'HIGH', 'MEDIUM', 'LOW'))
);

CREATE INDEX idx_tasks_project_id ON tasks (project_id);

-- FR-102: 태스크당 담당자 1명 이상(애플리케이션 레벨에서 검증), 다대다 연결
CREATE TABLE task_assignees (
    task_id BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id),
    PRIMARY KEY (task_id, user_id)
);

-- FR-102: 태스크 카드 내 체크리스트(서브태스크 항목)
CREATE TABLE task_checklist_items (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    content     VARCHAR(500) NOT NULL,
    is_checked  BOOLEAN NOT NULL DEFAULT false,
    position    INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_checklist_items_task_id ON task_checklist_items (task_id);
