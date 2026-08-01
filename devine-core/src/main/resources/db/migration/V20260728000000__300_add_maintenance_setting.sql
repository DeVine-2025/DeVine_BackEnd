-- 서버 점검 모드 설정
-- 설정은 전역으로 하나뿐이므로 id = 1인 단일 행만 존재한다.
-- CHECK 제약으로 두 번째 행 삽입을 스키마 수준에서 막고, 초기 행(점검 OFF)을 함께 넣어
-- 애플리케이션이 항상 findById(1)로 상태를 읽을 수 있게 한다.
CREATE TABLE maintenance_setting (
    id               BIGINT       PRIMARY KEY DEFAULT 1
        CONSTRAINT maintenance_setting_singleton CHECK (id = 1),
    enabled          BOOLEAN      NOT NULL DEFAULT FALSE,
    message          TEXT,
    estimated_end_at TIMESTAMP(6),
    created_at       TIMESTAMP(6) NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP(6),
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255)
);

INSERT INTO maintenance_setting (id, enabled) VALUES (1, FALSE);
