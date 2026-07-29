-- 주간 보고 항목의 "대분류"를 자유 등록형 CategoryKeyword(MAJOR)에서 실제 Project 참조로 교체한다.
-- V22/V23과 동일한 관례(개발 단계 데이터 보존 불필요, 기존 행을 지우고 컬럼을 교체)를 따른다.
-- middle_category_id(CategoryKeyword MIDDLE)는 그대로 유지하며, CategoryType.MAJOR enum 값과
-- category_keywords 테이블 자체는 다른 용도로 남아있을 수 있어 손대지 않는다.

DELETE FROM weekly_report_entries;

ALTER TABLE weekly_report_entries DROP COLUMN major_category_id;

ALTER TABLE weekly_report_entries
    ADD COLUMN project_id BIGINT NOT NULL REFERENCES projects (id);

CREATE INDEX idx_weekly_report_entries_project ON weekly_report_entries (project_id);
