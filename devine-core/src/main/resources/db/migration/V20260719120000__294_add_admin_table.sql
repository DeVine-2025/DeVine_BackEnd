-- admin 테이블 (관리자 인증/인가)
-- 관리자도 Clerk로 로그인하며, 이 테이블은 "누가 관리자인가"(clerk_id)와 권한 레벨을 관리한다.
CREATE TABLE admin (
    admin_id    BIGSERIAL    PRIMARY KEY,
    clerk_id    VARCHAR(255) NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    level       VARCHAR(30)  NOT NULL DEFAULT 'ADMIN',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    granted_by  VARCHAR(255),
    created_at  TIMESTAMP(6) NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP(6),
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    -- ⚠️ level 확장 시: AdminLevel enum에 값을 추가하면 이 CHECK도 새 마이그레이션으로 함께 갱신해야 한다.
    -- (누락 시 새 레벨 INSERT가 CHECK 위반 → 트랜잭션 abort로 이어져 로그인이 500으로 깨진다)
    CONSTRAINT admin_level_check CHECK (level IN ('ADMIN'))
);