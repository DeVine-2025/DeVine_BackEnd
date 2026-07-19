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
    CONSTRAINT admin_level_check CHECK (level IN ('ADMIN'))
);