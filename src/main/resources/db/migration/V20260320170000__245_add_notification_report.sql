-- notification type CHECK 제약조건에 리포트 관련 타입 추가
ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;
ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN ('MATCHING_APPLIED', 'MATCHING_PROPOSED', 'MATCHING_ACCEPTED',
                    'MATCHING_REJECTED', 'PROJECT_STATUS_CHANGED', 'PROJECT_MEMBER_JOINED',
                    'REPORT_COMPLETED', 'REPORT_FAILED'));
