-- 회원 하드삭제 시 credit_refund_request 행을 삭제하는 대신 member_id만 끊어서
-- 환불 청구/소멸 감사 기록을 영구 보존하기 위해 NOT NULL 제약을 제거한다.
ALTER TABLE credit_refund_request ALTER COLUMN member_id DROP NOT NULL;
