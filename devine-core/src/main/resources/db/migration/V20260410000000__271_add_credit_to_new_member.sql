-- 기존 회원 중 member_report_credit 행이 없는 회원에게 0 크레딧으로 행 생성
INSERT INTO member_report_credit (member_id, remaining_count)
SELECT m.member_id, 0
FROM member m
WHERE NOT EXISTS (
    SELECT 1 FROM member_report_credit mrc WHERE mrc.member_id = m.member_id
);
