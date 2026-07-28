-- FR-301 담당자 선택용 선행 요구사항 보완: 프로젝트별 멤버십 테이블이 없어
-- GET /api/projects/{projectId}/members가 "프로젝트 멤버" = "workspace 전체 User"로
-- 근사(fake)하던 문제를 해결한다. 실제 프로젝트-사용자 다대다 연결 테이블을 추가한다.
-- 프로젝트 생성 직후에는 생성자만 멤버여야 하므로, 기존 프로젝트도 backfill 시
-- "workspace 전체 User"가 아닌 created_by 한 명만 멤버로 채운다(동일 버그 재발 방지).

CREATE TABLE project_members (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects (id),
    user_id     BIGINT NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_project_members_project_user UNIQUE (project_id, user_id)
);

CREATE INDEX idx_project_members_project_id ON project_members (project_id);
CREATE INDEX idx_project_members_user_id ON project_members (user_id);

INSERT INTO project_members (project_id, user_id)
SELECT id, created_by FROM projects WHERE created_by IS NOT NULL;
