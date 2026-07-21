-- 회원 탈퇴 시각 기록용 컬럼 추가
-- 후속 작업(P3 익명화, P4 hard delete 배치)에서 deleted_at < now() - INTERVAL N DAY 조건으로 사용된다.

ALTER TABLE member ADD COLUMN deleted_at timestamp(6) without time zone;

-- hard delete 배치가 deleted_at으로 스캔하므로 인덱스 추가
CREATE INDEX idx_member_deleted_at ON member (deleted_at);
