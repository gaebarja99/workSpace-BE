-- 태스크 댓글(FR-305/FR-105-A) / 태스크 활동 로그(FR-105-B) 기능을 백엔드에서 완전히 제거한다.
-- 제품 담당자 확정(2026-07-28): 태스크 본체(생성/조회/수정/보드/리스트뷰/캘린더뷰)는 유지하되
-- 댓글/멘션/활동 로그는 더 이상 필요하지 않음. V22와 동일하게 CASCADE로 관련 테이블을 정리한다.

DROP TABLE IF EXISTS task_comment_mentions CASCADE;
DROP TABLE IF EXISTS task_activities CASCADE;
DROP TABLE IF EXISTS task_comments CASCADE;
