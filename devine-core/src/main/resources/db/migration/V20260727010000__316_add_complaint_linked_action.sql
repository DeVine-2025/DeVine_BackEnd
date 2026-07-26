-- 신고 처리 결과에 따른 연동 조치(프로젝트 비노출, 계정 정지 등)가 실제로 반영됐는지 표시한다.
-- 대상 유형(PROJECT/CHAT/DEVELOPER)과 무관한 단일 필드라 향후 SUSPEND 연동에도 그대로 사용된다.

ALTER TABLE complaint ADD COLUMN linked_action_completed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE complaint ADD COLUMN linked_action_at TIMESTAMP(6);
