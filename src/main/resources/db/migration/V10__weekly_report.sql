-- FR-401~404/408/410(P2 주간 보고 자동화, p2-weekly-report-contract.md).
-- 완료/진행/하이라이트/이슈 섹션은 어떤 테이블에도 스냅샷하지 않는다(WeeklyReportService가 매 요청마다
-- project(+user)+week_start~week_end 범위로 tasks/messages를 실시간 조회해 계산). 이 마이그레이션은
-- "보고서가 제출되었는가"(weekly_reports.status), "팀 보고서가 발행되었는가"(team_weekly_reports 존재 여부)
-- 두 사실과, FR-402 메시지 하이라이트 플래그만 저장한다.

-- FR-401/403: 개인 주간 보고서. (project_id, user_id, week_start) 단위로 하나만 존재.
CREATE TABLE weekly_reports (
    id             BIGSERIAL PRIMARY KEY,
    project_id     BIGINT NOT NULL REFERENCES projects (id),
    user_id        BIGINT NOT NULL REFERENCES users (id),
    week_start     DATE NOT NULL,
    week_end       DATE NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    next_week_plan TEXT NOT NULL DEFAULT '',
    submitted_at   TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_weekly_reports_status CHECK (status IN ('DRAFT', 'SUBMITTED')),
    CONSTRAINT uk_weekly_reports_project_user_week UNIQUE (project_id, user_id, week_start)
);

-- GET /reports/team, POST /reports/team/remind: 프로젝트+주차 범위 전체 조회 최적화.
CREATE INDEX idx_weekly_reports_project_id_week_start ON weekly_reports (project_id, week_start);

-- FR-404/408: 팀 발행 기록. (project_id, week_start) 존재 여부만으로 "발행 완료" vs "집계 중"을 판정한다.
CREATE TABLE team_weekly_reports (
    id            BIGSERIAL PRIMARY KEY,
    project_id    BIGINT NOT NULL REFERENCES projects (id),
    week_start    DATE NOT NULL,
    week_end      DATE NOT NULL,
    published_by  BIGINT NOT NULL REFERENCES users (id),
    published_at  TIMESTAMP NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_team_weekly_reports_project_week UNIQUE (project_id, week_start)
);

-- FR-402: 메시지 하이라이트(주간 보고 큐레이션). pinned/pinned_at(V5)과 동일한 형태로 추가한다.
ALTER TABLE messages ADD COLUMN highlighted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE messages ADD COLUMN highlighted_at TIMESTAMP;

-- FR-401 하이라이트 섹션 조회(project+주차+highlighted=true) 최적화.
CREATE INDEX idx_messages_highlighted ON messages (highlighted) WHERE highlighted = true;

-- FR-408: notifications.type CHECK 제약(V7)에 WEEKLY_REPORT_REMINDER 추가.
ALTER TABLE notifications DROP CONSTRAINT chk_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type
    CHECK (type IN ('TASK_DUE_SOON', 'TASK_DUE_TODAY', 'TASK_ASSIGNED', 'TASK_STATUS_CHANGED',
                     'WEEKLY_REPORT_REMINDER'));
