package com.umc.devine.domain.project.enums;

import lombok.Getter;

@Getter
public enum ProjectStatus {
    RECRUITING("모집 중"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
    DELETED("삭제됨");

    private final String displayName;

    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }
}