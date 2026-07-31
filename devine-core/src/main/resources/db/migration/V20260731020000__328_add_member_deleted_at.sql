-- Hard Delete 배치가 탈퇴 유예기간 경과 여부를 판단하기 위한 탈퇴 확정 시각 컬럼
ALTER TABLE member ADD COLUMN deleted_at TIMESTAMP(6);

-- 이 컬럼이 생기기 전에 이미 탈퇴 처리(강제탈퇴 확정 등)된 회원은 deleted_at이 NULL이라
-- Hard Delete 배치의 findByUsedAndDeletedAtBefore 조회에서 영원히 제외된다.
-- 정확한 탈퇴 확정 시각은 알 수 없으므로 updated_at(마지막 상태 변경 시각)을 근사치로 사용해 백필한다.
UPDATE member SET deleted_at = updated_at WHERE used = 'DELETED' AND deleted_at IS NULL;
