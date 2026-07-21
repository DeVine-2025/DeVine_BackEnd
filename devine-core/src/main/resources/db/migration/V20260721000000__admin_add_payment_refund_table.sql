-- payment_refund 테이블 (환불 상태 머신 + 어드민 감사)
CREATE TABLE payment_refund (
    refund_id        BIGSERIAL    PRIMARY KEY,
    payment_id       BIGINT       NOT NULL REFERENCES payment (payment_id),
    status           VARCHAR(20)  NOT NULL,
    reason           VARCHAR(255) NOT NULL,
    cancellation_id  VARCHAR(255),
    failure_reason   VARCHAR(255),
    created_at       TIMESTAMP(6) NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP(6),
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    CONSTRAINT payment_refund_status_check
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'UNKNOWN'))
);

CREATE INDEX idx_payment_refund_payment_id ON payment_refund (payment_id);

-- 동시성 방어: 결제건당 활성(비FAILED) 환불은 최대 1건.
-- 어드민 두 명이 동시에 눌러도 두 번째 INSERT가 여기서 막힌다.
CREATE UNIQUE INDEX ux_payment_refund_active
    ON payment_refund (payment_id)
    WHERE status IN ('IN_PROGRESS', 'COMPLETED', 'UNKNOWN');
