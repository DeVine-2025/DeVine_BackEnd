-- 프로젝트 마감 상태 추가: 마감일이 지났지만 대기 건이 남은 DEADLINE_PASSED, 모집을 마친 CLOSED
ALTER TABLE project DROP CONSTRAINT project_project_status_check;
ALTER TABLE project ADD CONSTRAINT project_project_status_check
    CHECK (project_status IN ('RECRUITING', 'DEADLINE_PASSED', 'CLOSED', 'IN_PROGRESS', 'COMPLETED', 'DELETED'));

-- 마감 관련 PM 알림, PM 제안 취소 알림
ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;
ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN ('MATCHING_APPLIED', 'MATCHING_PROPOSED', 'MATCHING_ACCEPTED',
                    'MATCHING_REJECTED', 'MATCHING_PROPOSAL_CANCELLED',
                    'PROJECT_STATUS_CHANGED', 'PROJECT_MEMBER_JOINED',
                    'PROJECT_DEADLINE_TODAY', 'PROJECT_DEADLINE_PASSED',
                    'REPORT_COMPLETED', 'REPORT_FAILED'));
