-- FR-204 파일 아카이브, FR-205 정보 아카이브(위키형 결정사항/참고자료).
-- tags는 콤마 이스케이프 문제를 피하기 위해 콤마 구분 컬럼 대신 순서를 보존하는 별도
-- 컬렉션 테이블(*_tags)로 저장한다. 태스크-메시지 연동(task_message_links, FR-301~305)은
-- 이번 마이그레이션 범위 밖이며 후속 마이그레이션에서 다룬다.

CREATE TABLE archive_items (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL REFERENCES projects (id),
    type         VARCHAR(20) NOT NULL,
    title        VARCHAR(200) NOT NULL,
    content      TEXT NOT NULL,
    author_id    BIGINT REFERENCES users (id),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_archive_items_type CHECK (type IN ('DECISION', 'REFERENCE'))
);

CREATE INDEX idx_archive_items_project_id ON archive_items (project_id);

CREATE TABLE archive_item_tags (
    archive_item_id BIGINT NOT NULL REFERENCES archive_items (id) ON DELETE CASCADE,
    position        INT NOT NULL,
    tag             VARCHAR(100) NOT NULL,
    PRIMARY KEY (archive_item_id, position)
);

-- FR-204: 실제 바이트는 로컬 디스크(후속 S3 교체 가능, teamsync.storage.local-base-path)에 저장하고
-- 이 테이블은 메타데이터만 보관한다. storage_key는 서버가 생성한 UUID 기반 내부 파일명으로,
-- original_filename을 디스크 경로에 그대로 쓰지 않아 경로 순회/파일명 충돌을 방지한다.
CREATE TABLE archived_files (
    id                BIGSERIAL PRIMARY KEY,
    project_id        BIGINT NOT NULL REFERENCES projects (id),
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(255),
    size_bytes        BIGINT NOT NULL,
    storage_key       VARCHAR(255) NOT NULL,
    uploader_id       BIGINT NOT NULL REFERENCES users (id),
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_archived_files_storage_key UNIQUE (storage_key)
);

CREATE INDEX idx_archived_files_project_id ON archived_files (project_id);

CREATE TABLE archived_file_tags (
    archived_file_id BIGINT NOT NULL REFERENCES archived_files (id) ON DELETE CASCADE,
    position         INT NOT NULL,
    tag              VARCHAR(100) NOT NULL,
    PRIMARY KEY (archived_file_id, position)
);
