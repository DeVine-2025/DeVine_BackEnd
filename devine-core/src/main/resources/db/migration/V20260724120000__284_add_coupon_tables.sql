-- coupon 테이블 (쿠폰 정책/캠페인, 관리자가 생성)
CREATE TABLE
    coupon (
        coupon_id BIGSERIAL PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        discount_type VARCHAR(20) NOT NULL,
        discount_value BIGINT NOT NULL,
        applicable_ticket_product_id BIGINT REFERENCES ticket_product (ticket_product_id),
        total_issue_limit INTEGER,
        issued_count INTEGER NOT NULL DEFAULT 0,
        used_count INTEGER NOT NULL DEFAULT 0,
        valid_from TIMESTAMP(6) NOT NULL,
        valid_until TIMESTAMP(6) NOT NULL,
        is_active BOOLEAN NOT NULL DEFAULT TRUE,
        description VARCHAR(255),
        created_at TIMESTAMP(6) NOT NULL DEFAULT now (),
        updated_at TIMESTAMP(6),
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        CONSTRAINT coupon_discount_type_check CHECK (discount_type IN ('FIXED_RATE', 'FIXED_AMOUNT')),
        CONSTRAINT coupon_discount_value_check CHECK (discount_value > 0),
        CONSTRAINT coupon_valid_period_check CHECK (valid_from < valid_until)
    );

-- coupon_code 테이블 (배치 생성 또는 관리자 직접 입력된 코드, '코드생성' 발급 방식에서만 사용)
-- max_uses가 NULL이면 무제한, N이면 서로 다른 회원 N명까지 각 1회씩 등록 가능한 공유 코드
CREATE TABLE
    coupon_code (
        coupon_code_id BIGSERIAL PRIMARY KEY,
        coupon_id BIGINT NOT NULL REFERENCES coupon (coupon_id),
        code VARCHAR(30) NOT NULL UNIQUE,
        max_uses INTEGER,
        used_count INTEGER NOT NULL DEFAULT 0,
        created_at TIMESTAMP(6) NOT NULL DEFAULT now (),
        updated_at TIMESTAMP(6),
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        CONSTRAINT coupon_code_max_uses_check CHECK (max_uses IS NULL OR max_uses > 0),
        CONSTRAINT coupon_code_used_count_check CHECK (used_count >= 0)
    );

-- member_coupon 테이블 (회원이 실제로 보유한 쿠폰)
CREATE TABLE
    member_coupon (
        member_coupon_id BIGSERIAL PRIMARY KEY,
        member_id BIGINT NOT NULL REFERENCES member (member_id),
        coupon_id BIGINT NOT NULL REFERENCES coupon (coupon_id),
        coupon_code_id BIGINT REFERENCES coupon_code (coupon_code_id),
        status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
        used_at TIMESTAMP(6),
        payment_id BIGINT REFERENCES payment (payment_id),
        created_at TIMESTAMP(6) NOT NULL DEFAULT now (),
        updated_at TIMESTAMP(6),
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        CONSTRAINT member_coupon_status_check CHECK (status IN ('AVAILABLE', 'USED', 'EXPIRED')),
        CONSTRAINT member_coupon_unique_coupon_member UNIQUE (coupon_id, member_id)
    );

-- 인덱스
CREATE INDEX idx_member_coupon_member_id ON member_coupon (member_id);

CREATE INDEX idx_member_coupon_coupon_id ON member_coupon (coupon_id);

CREATE INDEX idx_member_coupon_status ON member_coupon (status);

CREATE INDEX idx_coupon_code_coupon_id ON coupon_code (coupon_id);
