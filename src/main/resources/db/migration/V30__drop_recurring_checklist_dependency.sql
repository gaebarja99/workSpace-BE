-- 반복 태스크 템플릿(recurring_task_templates, FR-106), 태스크 체크리스트(task_checklist_items, FR-102 서브셋),
-- 태스크 간 의존관계(task_dependencies, FR-107) 기능을 제품 결정으로 완전히 제거한다. 히스토리 보존을 위해
-- 기존 V3__task.sql/V15__recurring_task_template.sql/V17__task_dependencies.sql은 수정하지 않고,
-- 이 마이그레이션에서 해당 테이블/컬럼만 되돌린다.

-- 1) task_dependencies(FR-107): 다른 테이블이 이를 참조하지 않으므로 단순 DROP.
DROP TABLE IF EXISTS task_dependencies;

-- 2) task_checklist_items(FR-102 체크리스트): 다른 테이블이 이를 참조하지 않으므로 단순 DROP.
DROP TABLE IF EXISTS task_checklist_items;

-- 3) recurring_task_templates(FR-106): tasks.recurring_template_id가 이 테이블을 참조하므로(V15),
--    템플릿 하위 테이블 → tasks의 참조 컬럼 → 템플릿 본체 순으로 제거한다.
DROP TABLE IF EXISTS recurring_task_template_assignees;
ALTER TABLE tasks DROP COLUMN IF EXISTS recurring_template_id;
DROP TABLE IF EXISTS recurring_task_templates;
