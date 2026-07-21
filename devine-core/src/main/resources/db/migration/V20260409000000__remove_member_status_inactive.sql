-- MemberStatus.INACTIVE 제거
-- 과거 회원가입 플로우(빈 user row 선생성 → 가입 완료 시 ACTIVE 전환)에서 사용되던 상태이며,
-- 현재 가입 플로우에서는 사용되지 않으므로 enum 값에서 제외한다.
-- 참고: 사전 확인 결과 member.used = 'INACTIVE'인 데이터는 존재하지 않는다.

ALTER TABLE member DROP CONSTRAINT member_used_check;
ALTER TABLE member ADD CONSTRAINT member_used_check
    CHECK (used IN ('ACTIVE', 'DELETED'));
