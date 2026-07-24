-- 프로젝트 관리(관리자, P2): 프로젝트 진행 상태(ACTIVE/PLANNED/ARCHIVED) 컬럼 추가.
-- ddl-auto=validate이므로 이 스키마가 Project 엔티티(ProjectStatus)와 정확히 일치해야 한다.
-- 기존 프로젝트 row는 전부 ACTIVE로 채워지도록 DEFAULT 'ACTIVE' 후 NOT NULL로 확정한다.

ALTER TABLE projects ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE projects ADD CONSTRAINT chk_projects_status CHECK (status IN ('ACTIVE', 'PLANNED', 'ARCHIVED'));

CREATE INDEX idx_projects_workspace_id_status ON projects (workspace_id, status);
