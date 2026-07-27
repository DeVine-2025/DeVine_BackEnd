-- 이 신고 처리로 연동 조치(프로젝트 비노출, 계정 정지 등)가 실행된 적이 있는지 표시하는 실행 이력이다.
-- 현재 상태의 거울이 아니다. 이후 관리자가 프로젝트를 다시 노출시켜도 실행 사실은 변하지 않으므로 TRUE로 남으며,
-- 프로젝트의 현재 노출 상태는 project.is_hidden으로 판단해야 한다.
-- 대상 유형(PROJECT/CHAT/DEVELOPER)과 무관한 단일 필드라 향후 SUSPEND 연동에도 그대로 사용된다.

ALTER TABLE complaint ADD COLUMN linked_action_completed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE complaint ADD COLUMN linked_action_at TIMESTAMP(6);
