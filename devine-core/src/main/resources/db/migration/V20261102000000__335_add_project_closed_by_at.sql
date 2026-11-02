-- 프로젝트 마감 처리자·처리 시각. closed_by가 비어 있으면 자동 마감(배치·마감 확인 훅)
ALTER TABLE project ADD COLUMN closed_by BIGINT REFERENCES member (member_id);
ALTER TABLE project ADD COLUMN closed_at TIMESTAMP(6);
