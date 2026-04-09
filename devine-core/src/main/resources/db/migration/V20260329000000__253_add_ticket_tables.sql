-- ticket_product 테이블 (상품 카탈로그)
CREATE TABLE
    ticket_product (
        ticket_product_id BIGSERIAL PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        price BIGINT NOT NULL,
        credit_amount INTEGER NOT NULL,
        active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP(6) NOT NULL DEFAULT now (),
        updated_at TIMESTAMP(6),
        created_by VARCHAR(255),
        updated_by VARCHAR(255)
    );

-- payment_ticket 테이블 (결제 ↔ 상품 연결, 구매 수량 포함)
CREATE TABLE
    payment_ticket (
        payment_ticket_id BIGSERIAL PRIMARY KEY,
        payment_id BIGINT NOT NULL REFERENCES payment (payment_id),
        ticket_product_id BIGINT NOT NULL REFERENCES ticket_product (ticket_product_id),
        quantity INTEGER NOT NULL,
        unit_price BIGINT NOT NULL,
        unit_credit_amount INTEGER NOT NULL,
        created_at TIMESTAMP(6) NOT NULL DEFAULT now (),
        updated_at TIMESTAMP(6),
        created_by VARCHAR(255),
        updated_by VARCHAR(255)
    );

-- member_report_credit 테이블 (사용자별 잔여 생성권)
CREATE TABLE
    member_report_credit (
        member_report_credit_id BIGSERIAL PRIMARY KEY,
        member_id BIGINT NOT NULL UNIQUE REFERENCES member (member_id),
        remaining_count INTEGER NOT NULL DEFAULT 0,
        created_at TIMESTAMP(6) NOT NULL DEFAULT now (),
        updated_at TIMESTAMP(6),
        created_by VARCHAR(255),
        updated_by VARCHAR(255)
    );

-- 인덱스
CREATE INDEX idx_payment_ticket_payment_id ON payment_ticket (payment_id);

-- 초기 상품 데이터
INSERT INTO
    ticket_product (name, price, credit_amount)
VALUES
    ('리포트 생성권 1개', 4900, 1),
    ('리포트 생성권 3개', 9900, 3);