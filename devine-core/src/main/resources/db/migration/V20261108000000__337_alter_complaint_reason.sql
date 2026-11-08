ALTER TABLE complaint RENAME COLUMN reason TO detail;
ALTER TABLE complaint ALTER COLUMN detail DROP NOT NULL;

ALTER TABLE complaint ADD COLUMN reason_code VARCHAR(30);
UPDATE complaint SET reason_code = 'ETC' WHERE reason_code IS NULL;
ALTER TABLE complaint ALTER COLUMN reason_code SET NOT NULL;

ALTER TABLE complaint ADD COLUMN notify_summary TEXT;

CREATE UNIQUE INDEX uk_complaint_complainant_target
    ON complaint (complainant_member_id, target_type, target_id)
    WHERE target_id IS NOT NULL;

CREATE INDEX idx_complaint_complainant_created ON complaint (complainant_member_id, created_at);

ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;
ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN ('MATCHING_APPLIED', 'MATCHING_PROPOSED', 'MATCHING_ACCEPTED',
                    'MATCHING_REJECTED', 'MATCHING_PROPOSAL_CANCELLED',
                    'PROJECT_STATUS_CHANGED', 'PROJECT_MEMBER_JOINED',
                    'PROJECT_DEADLINE_TODAY', 'PROJECT_DEADLINE_PASSED',
                    'REPORT_COMPLETED', 'REPORT_FAILED',
                    'COMPLAINT_PROJECT_BLINDED', 'COMPLAINT_RESOLVED_COMPLAINANT',
                    'COMPLAINT_RESOLVED_RESPONDENT'));
