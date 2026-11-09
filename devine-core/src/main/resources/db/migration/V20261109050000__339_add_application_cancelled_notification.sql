-- 회원 탈퇴 전 대기 건 일괄 취소 시 상대방에게 보내는 지원 취소 알림
ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;
ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN ('MATCHING_APPLIED', 'MATCHING_PROPOSED', 'MATCHING_ACCEPTED',
                    'MATCHING_REJECTED', 'MATCHING_PROPOSAL_CANCELLED', 'MATCHING_APPLICATION_CANCELLED',
                    'PROJECT_STATUS_CHANGED', 'PROJECT_MEMBER_JOINED',
                    'PROJECT_DEADLINE_TODAY', 'PROJECT_DEADLINE_PASSED',
                    'REPORT_COMPLETED', 'REPORT_FAILED'));
