-- FR-405(P3, 보고서 템플릿 관리): 조직(워크스페이스)/팀(프로젝트)별 커스텀 주간보고 양식.
-- project_id가 NULL이면 워크스페이스 전사 기본 템플릿(워크스페이스당 1개), NOT NULL이면 해당
-- 프로젝트(팀) 전용 템플릿(프로젝트당 1개)이다. 실제 주간보고 작성 화면(FR-401~404)과는 아직
-- 연동하지 않는다(이번 세션 의도된 축소 범위 — HANDOFF 한계 항목 참고).
-- 섹션 4종(완료/진행중/하이라이트/이슈)의 표준 키는 WeeklyReportService의 자동 계산 섹션과
-- 이름을 맞춘 것이며, MANUAL은 자동 계산 없이 사용자가 직접 채우는 자유 섹션이다.

CREATE TABLE report_templates (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces (id),
    project_id   BIGINT REFERENCES projects (id) ON DELETE CASCADE,
    name         VARCHAR(200) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

-- 워크스페이스당 전사 기본 템플릿(project_id IS NULL)은 최대 1개.
CREATE UNIQUE INDEX uk_report_templates_workspace_default ON report_templates (workspace_id) WHERE project_id IS NULL;

-- 프로젝트당 전용 템플릿(project_id IS NOT NULL)은 최대 1개.
CREATE UNIQUE INDEX uk_report_templates_project ON report_templates (project_id) WHERE project_id IS NOT NULL;

CREATE TABLE report_template_sections (
    id          BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES report_templates (id) ON DELETE CASCADE,
    section_key VARCHAR(20) NOT NULL,
    title       VARCHAR(200) NOT NULL,
    order_index INT NOT NULL,
    auto_filled BOOLEAN NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_report_template_sections_section_key
        CHECK (section_key IN ('COMPLETED', 'IN_PROGRESS', 'HIGHLIGHTS', 'ISSUES', 'MANUAL'))
);

CREATE INDEX idx_report_template_sections_template_id ON report_template_sections (template_id);

-- 표준 섹션 키(MANUAL 제외)는 템플릿 하나에 같은 키가 중복될 수 없다(MANUAL은 여러 개 허용).
CREATE UNIQUE INDEX uk_report_template_sections_template_key
    ON report_template_sections (template_id, section_key) WHERE section_key <> 'MANUAL';
