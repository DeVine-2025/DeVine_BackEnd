-- 기존 CHECK 제약 삭제 후 확장된 제약 추가 (환불/취소 상태 지원)
ALTER TABLE transaction DROP CONSTRAINT transaction_type_check;
ALTER TABLE transaction ADD CONSTRAINT transaction_type_check CHECK (type IN ('PAYMENT', 'REFUND'));

ALTER TABLE transaction DROP CONSTRAINT transaction_status_check;
ALTER TABLE transaction ADD CONSTRAINT transaction_status_check CHECK (status IN ('PAID', 'FAILED', 'REFUNDED', 'CANCELLED'));
