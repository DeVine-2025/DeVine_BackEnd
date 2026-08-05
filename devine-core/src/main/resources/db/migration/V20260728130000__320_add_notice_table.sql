-- notice 테이블 (관리자가 등록하는 공지사항)
-- display_start_at / display_end_at 이 NULL이면 해당 방향으로는 기간 제한이 없다(둘 다 NULL이면 상시 노출).
-- is_exposed 는 기간과 독립된 수동 노출 스위치이며, 최종 노출 여부는 두 조건의 AND로 판정한다.
CREATE TABLE
    notice (
        notice_id BIGSERIAL PRIMARY KEY,
        title VARCHAR(100) NOT NULL,
        content TEXT NOT NULL,
        display_start_at TIMESTAMP(6),
        display_end_at TIMESTAMP(6),
        is_exposed BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP(6) NOT NULL DEFAULT now (),
        updated_at TIMESTAMP(6),
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        CONSTRAINT notice_display_period_check CHECK (
            display_start_at IS NULL
            OR display_end_at IS NULL
            OR display_start_at < display_end_at
        )
    );

-- 관리자/유저 목록 모두 최신순 정렬이라 created_at 내림차순 인덱스를 둔다.
CREATE INDEX idx_notice_created_at ON notice (created_at DESC);