-- external_integration_health 테이블 (외부 연동별 최신 헬스체크 스냅샷)
-- 연동 하나당 한 행만 유지하며 점검할 때마다 덮어쓴다(이력 미보관).
CREATE TABLE
    external_integration_health (
        external_integration_health_id BIGSERIAL PRIMARY KEY,
        integration_type VARCHAR(30) NOT NULL UNIQUE,
        status VARCHAR(20) NOT NULL,
        response_time_ms BIGINT,
        checked_at TIMESTAMP(6) NOT NULL,
        error_message VARCHAR(500),
        created_at TIMESTAMP(6) NOT NULL DEFAULT now (),
        updated_at TIMESTAMP(6),
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        CONSTRAINT external_integration_health_type_check CHECK (
            integration_type IN (
                'CLERK_API',
                'CLERK_JWKS',
                'GITHUB_API',
                'FASTAPI_AI',
                'GEMINI',
                'OPENAI',
                'PORTONE'
            )
        ),
        CONSTRAINT external_integration_health_status_check CHECK (
            status IN ('NORMAL', 'DELAYED', 'DOWN', 'UNKNOWN')
        ),
        CONSTRAINT external_integration_health_response_time_check CHECK (
            response_time_ms IS NULL
            OR response_time_ms >= 0
        )
    );
