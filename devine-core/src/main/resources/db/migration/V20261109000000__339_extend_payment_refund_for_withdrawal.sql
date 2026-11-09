-- 회원 탈퇴 시 남은 생성권 환불 신청(FUNC-MEM-004)을 payment_refund에 함께 기록한다.
-- 기존 관리자 환불(#302)은 결제 전체 취소라 환불 금액/수량을 따로 두지 않았고, 기존 행은 source = 'ADMIN'으로 채운다.

-- 탈퇴 환불 신청 상태(REQUESTED): 운영자가 실제 부분 취소를 마치면 COMPLETED로 바뀐다.
ALTER TABLE payment_refund DROP CONSTRAINT payment_refund_status_check;
ALTER TABLE payment_refund ADD CONSTRAINT payment_refund_status_check
    CHECK (status IN ('REQUESTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'UNKNOWN'));

-- 신청 경로: ADMIN(관리자 결제 전체 환불), WITHDRAWAL(회원 탈퇴 환불 신청)
ALTER TABLE payment_refund ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'ADMIN';
ALTER TABLE payment_refund ADD CONSTRAINT payment_refund_source_check
    CHECK (source IN ('ADMIN', 'WITHDRAWAL'));

-- 환불 금액/수량: 탈퇴 환불은 이 금액만 부분 취소한다. 기존 관리자 환불 행은 NULL(결제 전체 환불)이다.
ALTER TABLE payment_refund ADD COLUMN refund_amount BIGINT;
ALTER TABLE payment_refund ADD COLUMN refund_credit_count INTEGER;

-- 환불 연락 이메일(결정 F): 환불이 끝나면 지운다.
ALTER TABLE payment_refund ADD COLUMN contact_email VARCHAR(255);
