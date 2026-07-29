-- 프로젝트 생성 화면에서 마감일을 입력받을 수 있도록 projects에 deadline 컬럼을 추가한다.
-- 태스크의 due_date와 마찬가지로 선택 입력이라 NULL을 허용한다.

ALTER TABLE projects ADD COLUMN deadline DATE;
