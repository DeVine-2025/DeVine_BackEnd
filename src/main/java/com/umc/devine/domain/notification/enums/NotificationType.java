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

    // 프로젝트 관련
    PROJECT_STATUS_CHANGED("프로젝트 상태가 변경되었습니다", "project"),
    PROJECT_MEMBER_JOINED("새 팀원이 합류했습니다", "project");

    private final String defaultTitle;
    private final String category;
}
