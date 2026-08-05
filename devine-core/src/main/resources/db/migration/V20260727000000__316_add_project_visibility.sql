-- 프로젝트 노출 여부를 라이프사이클 상태(project_status)와 분리해 별도 플래그로 관리한다.
-- 기존에는 비노출을 project_status = 'HIDDEN'으로 덮어써서 원래 상태(RECRUITING/IN_PROGRESS/COMPLETED)가
-- 유실돼 다시 노출로 되돌릴 수 없었다.

ALTER TABLE project ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE project ADD COLUMN visibility_changed_by BIGINT REFERENCES member (member_id);
ALTER TABLE project ADD COLUMN visibility_changed_at TIMESTAMP(6);

-- 기존 HIDDEN 행을 플래그로 이관.
-- 원래 라이프사이클 상태를 보관한 적이 없어 RECRUITING으로 복원한다.
-- HIDDEN은 V20260726000000(#297)에서 도입돼 하루만 존재했고 dev 환경에서만 사용됐으므로 영향 데이터는 사실상 없다.
UPDATE project SET is_hidden = TRUE, project_status = 'RECRUITING' WHERE project_status = 'HIDDEN';

ALTER TABLE project DROP CONSTRAINT project_project_status_check;

ALTER TABLE project ADD CONSTRAINT project_project_status_check
    CHECK (((project_status)::text = ANY (ARRAY[('RECRUITING'::character varying)::text, ('IN_PROGRESS'::character varying)::text, ('COMPLETED'::character varying)::text, ('DELETED'::character varying)::text])));
