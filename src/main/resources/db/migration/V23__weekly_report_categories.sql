-- 주간 보고 전면 재설계: 태스크 자동 취합(FR-401) -> 대/중/소분류 + 달성율 수동 입력 방식으로 교체.
-- V22__drop_removed_features.sql과 동일한 관례(기능 교체 시 과거 테이블을 히스토리 보존 없이 CASCADE
-- drop)를 따른다. FR-405(보고서 템플릿)도 이번에 함께 폐기한다(에디터에 연동된 적 없는 죽은 기능이며
-- 새 고정 컬럼 표 형식과 개념이 맞지 않음). FR-404(팀 보고 "발행") 개념도 제거 — 팀장/대표 뷰는 이제
-- 스냅샷 없이 항상 실시간 집계한다.

DROP TABLE IF EXISTS weekly_reports CASCADE;
DROP TABLE IF EXISTS team_weekly_reports CASCADE;
DROP TABLE IF EXISTS report_templates CASCADE;
DROP TABLE IF EXISTS report_template_sections CASCADE;

-- ----- 대/중분류 표준 키워드 -----
-- 워크스페이스의 모든 인증 사용자가 추가/수정 가능(관리자 전용 아님, CategoryKeywordController 참고).
-- created_by가 NULL인 항목은 이번 마이그레이션이 심은 표준 시드 데이터다.
CREATE TABLE category_keywords (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(10) NOT NULL CHECK (type IN ('MAJOR', 'MIDDLE')),
    name        VARCHAR(100) NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_by  BIGINT REFERENCES users (id),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- 활성 항목 내에서만 (type, name) 유일 — soft delete(active=false) 후 같은 이름으로 재생성 가능하게 한다.
CREATE UNIQUE INDEX uk_category_keywords_type_name_active
    ON category_keywords (type, name) WHERE active;

INSERT INTO category_keywords (type, name, order_index, active, created_by) VALUES
    ('MAJOR', 'rCMS (당원관리)', 0, TRUE, NULL),
    ('MAJOR', '다이렉트몰 & 추천고객', 1, TRUE, NULL),
    ('MAJOR', '공공와이파이', 2, TRUE, NULL),
    ('MAJOR', '출입통제시스템 (EOS)', 3, TRUE, NULL),
    ('MAJOR', '국립항공박물관 (유지보수)', 4, TRUE, NULL),
    ('MAJOR', '무인매장', 5, TRUE, NULL),
    ('MAJOR', 'NIA 관련 사업', 6, TRUE, NULL),
    ('MAJOR', '기타 / 공통 업무', 7, TRUE, NULL);

INSERT INTO category_keywords (type, name, order_index, active, created_by) VALUES
    ('MIDDLE', '분석', 0, TRUE, NULL),
    ('MIDDLE', '설계', 1, TRUE, NULL),
    ('MIDDLE', '기준정보', 2, TRUE, NULL),
    ('MIDDLE', '개발/구현', 3, TRUE, NULL),
    ('MIDDLE', '테스트/품질 검증', 4, TRUE, NULL),
    ('MIDDLE', 'MR지원/유지보수', 5, TRUE, NULL),
    ('MIDDLE', '운영지원', 6, TRUE, NULL),
    ('MIDDLE', '제안서/기술 문서 정리', 7, TRUE, NULL),
    ('MIDDLE', '현장조사', 8, TRUE, NULL),
    ('MIDDLE', '감사·로그 관리', 9, TRUE, NULL);

-- ----- 개인 주간 보고서: project_id 종속 제거, user_id + week_start 단위 1개 -----
CREATE TABLE weekly_reports (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users (id),
    week_start   DATE NOT NULL,
    week_end     DATE NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED')),
    submitted_at TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_weekly_reports_user_week UNIQUE (user_id, week_start)
);

-- ----- 보고서 행(대/중/소분류 + 상세업무 + 달성율) -----
CREATE TABLE weekly_report_entries (
    id                 BIGSERIAL PRIMARY KEY,
    report_id          BIGINT NOT NULL REFERENCES weekly_reports (id) ON DELETE CASCADE,
    section            VARCHAR(20) NOT NULL CHECK (section IN ('THIS_WEEK', 'NEXT_WEEK')),
    major_category_id  BIGINT NOT NULL REFERENCES category_keywords (id),
    middle_category_id BIGINT NOT NULL REFERENCES category_keywords (id),
    minor_category     VARCHAR(255) NOT NULL DEFAULT '',
    detail             TEXT NOT NULL DEFAULT '',
    rate_percent       SMALLINT NOT NULL DEFAULT 0 CHECK (rate_percent BETWEEN 0 AND 100),
    order_index        INT NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_weekly_report_entries_report_section_order
    ON weekly_report_entries (report_id, section, order_index);
