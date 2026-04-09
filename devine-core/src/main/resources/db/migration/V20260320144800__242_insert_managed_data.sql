-- ===========================================
-- 1. Fix dev_report partial unique index
-- ===========================================
-- 실패한 리포트(error_message IS NOT NULL)는 제외하여 재시도 가능하도록 변경
ALTER TABLE ONLY dev_report DROP CONSTRAINT IF EXISTS uk_dev_report_git_repo_type;
DROP INDEX IF EXISTS uk_dev_report_git_repo_type;

CREATE UNIQUE INDEX uk_dev_report_git_repo_type
    ON dev_report (git_repo_id, report_type)
    WHERE error_message IS NULL;

-- ===========================================
-- 2. Add missing CHECK constraints for enum columns
-- ===========================================
-- 기존 제약조건이 있으면 제거 후 재생성

ALTER TABLE dev_techstack DROP CONSTRAINT IF EXISTS dev_techstack_source_check;
ALTER TABLE dev_techstack ADD CONSTRAINT dev_techstack_source_check
    CHECK (source IN ('AUTO', 'MANUAL'));

ALTER TABLE dev_report DROP CONSTRAINT IF EXISTS dev_report_report_type_check;
ALTER TABLE dev_report ADD CONSTRAINT dev_report_report_type_check
    CHECK (report_type IN ('MAIN', 'DETAIL'));

ALTER TABLE dev_report DROP CONSTRAINT IF EXISTS dev_report_visibility_check;
ALTER TABLE dev_report ADD CONSTRAINT dev_report_visibility_check
    CHECK (visibility IN ('PUBLIC', 'PRIVATE'));

ALTER TABLE report_embedding DROP CONSTRAINT IF EXISTS report_embedding_status_check;
ALTER TABLE report_embedding ADD CONSTRAINT report_embedding_status_check
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'));

ALTER TABLE project_embedding DROP CONSTRAINT IF EXISTS project_embedding_status_check;
ALTER TABLE project_embedding ADD CONSTRAINT project_embedding_status_check
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'));

ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;
ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN ('MATCHING_APPLIED', 'MATCHING_PROPOSED', 'MATCHING_ACCEPTED', 'MATCHING_REJECTED', 'PROJECT_STATUS_CHANGED', 'PROJECT_MEMBER_JOINED'));

ALTER TABLE matching DROP CONSTRAINT IF EXISTS matching_decision_check;
ALTER TABLE matching ADD CONSTRAINT matching_decision_check
    CHECK (decision IN ('PENDING', 'ACCEPT', 'REJECT'));

ALTER TABLE matching DROP CONSTRAINT IF EXISTS matching_part_check;
ALTER TABLE matching ADD CONSTRAINT matching_part_check
    CHECK (part IN ('PM', 'FRONTEND', 'BACKEND', 'INFRA'));

ALTER TABLE contact DROP CONSTRAINT IF EXISTS contact_type_check;
ALTER TABLE contact ADD CONSTRAINT contact_type_check
    CHECK (type IN ('EMAIL', 'LINKEDIN', 'GITHUB'));

ALTER TABLE image DROP CONSTRAINT IF EXISTS image_image_type_check;
ALTER TABLE image ADD CONSTRAINT image_image_type_check
    CHECK (image_type IN ('PROFILE', 'PROJECT', 'EDITOR'));

ALTER TABLE project DROP CONSTRAINT IF EXISTS project_duration_range_check;
ALTER TABLE project ADD CONSTRAINT project_duration_range_check
    CHECK (duration_range IN ('UNDER_ONE', 'ONE_TO_THREE', 'THREE_TO_SIX', 'SIX_PLUS'));

ALTER TABLE project_requirement_member DROP CONSTRAINT IF EXISTS project_requirement_member_part_check;
ALTER TABLE project_requirement_member ADD CONSTRAINT project_requirement_member_part_check
    CHECK (req_mem_part IN ('PM', 'FRONTEND', 'BACKEND', 'INFRA'));
