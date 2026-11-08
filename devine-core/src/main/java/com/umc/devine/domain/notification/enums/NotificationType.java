package com.umc.devine.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    // 매칭 관련
    MATCHING_APPLIED("새로운 지원자가 있습니다", "matching"),
    MATCHING_PROPOSED("프로젝트 제안이 도착했습니다", "matching"),
    MATCHING_ACCEPTED("지원이 수락되었습니다", "matching"),
    MATCHING_REJECTED("지원이 거절되었습니다", "matching"),
    MATCHING_PROPOSAL_CANCELLED("프로젝트 제안이 취소되었습니다", "matching"),

    // 프로젝트 관련
    PROJECT_STATUS_CHANGED("프로젝트 상태가 변경되었습니다", "project"),
    PROJECT_MEMBER_JOINED("새 팀원이 합류했습니다", "project"),
    PROJECT_DEADLINE_TODAY("오늘 모집이 마감됩니다", "project"),
    PROJECT_DEADLINE_PASSED("모집 마감일이 지났습니다", "project"),

    // 리포트 관련
    REPORT_COMPLETED("리포트 생성이 완료되었습니다", "report"),
    REPORT_FAILED("리포트 생성에 실패했습니다", "report"),

    COMPLAINT_PROJECT_BLINDED("신고가 접수돼 프로젝트를 가렸습니다", "complaint"),
    COMPLAINT_RESOLVED_COMPLAINANT("신고 검토 결과를 알려드립니다", "complaint"),
    COMPLAINT_RESOLVED_RESPONDENT("운영 정책 관련 안내입니다", "complaint");

    private final String defaultTitle;
    private final String category;
}
