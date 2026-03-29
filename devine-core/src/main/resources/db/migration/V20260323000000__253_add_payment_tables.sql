-- payment 테이블
CREATE TABLE payment (
    payment_id          BIGSERIAL PRIMARY KEY,
    portone_payment_id  VARCHAR(255) NOT NULL UNIQUE,
    member_id           BIGINT       NOT NULL REFERENCES member (member_id),
    order_name          VARCHAR(255) NOT NULL,
    amount              BIGINT       NOT NULL,
    currency            VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP(6),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

-- transaction 테이블
CREATE TABLE transaction (
    transaction_id          BIGSERIAL PRIMARY KEY,
    portone_transaction_id  VARCHAR(255) NOT NULL UNIQUE,
    payment_id              BIGINT       NOT NULL REFERENCES payment (payment_id),
    type                    VARCHAR(30)  NOT NULL,
    status                  VARCHAR(30)  NOT NULL,
    method                  VARCHAR(50)  NOT NULL,
    pg_provider             VARCHAR(50)  NOT NULL DEFAULT 'UNKNOWN',
    amount                  BIGINT       NOT NULL,
    paid_at                 TIMESTAMP(6),
    created_at              TIMESTAMP(6) NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP(6),
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    CONSTRAINT transaction_type_check   CHECK (type   IN ('PAYMENT')),
    CONSTRAINT transaction_status_check CHECK (status IN ('PAID', 'FAILED'))
);

-- card_detail 테이블 (transaction과 1:1, PK 공유)
CREATE TABLE card_detail (
    id                  BIGINT       PRIMARY KEY REFERENCES transaction (transaction_id),
    card_name           VARCHAR(100),
    card_number         VARCHAR(50),
    card_brand          VARCHAR(50),
    approval_number     VARCHAR(50),
    installment_month   INTEGER,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP(6),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

-- easy_pay_detail 테이블 (transaction과 1:1, PK 공유)
CREATE TABLE easy_pay_detail (
    id                  BIGINT       PRIMARY KEY REFERENCES transaction (transaction_id),
    provider            VARCHAR(100),
    card_name           VARCHAR(100),
    card_number         VARCHAR(50),
    card_brand          VARCHAR(50),
    approval_number     VARCHAR(50),
    installment_month   INTEGER,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP(6),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

-- 인덱스
CREATE INDEX idx_payment_member_id ON payment (member_id);
CREATE INDEX idx_transaction_payment_id ON transaction (payment_id);
