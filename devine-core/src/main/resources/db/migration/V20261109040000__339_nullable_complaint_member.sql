-- 탈퇴 회원이 신고자/피신고자인 신고는 행을 남기고 식별 칸만 비운다.
ALTER TABLE complaint ALTER COLUMN complainant_member_id DROP NOT NULL;
ALTER TABLE complaint ALTER COLUMN respondent_member_id DROP NOT NULL;
