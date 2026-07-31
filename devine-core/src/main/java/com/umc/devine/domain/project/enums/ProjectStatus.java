package com.umc.devine.domain.project.enums;

import lombok.Getter;

import java.util.List;

@Getter
public enum ProjectStatus {
    RECRUITING("모집 중"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
    DELETED("삭제됨"),
    HIDDEN("숨김 처리됨");

    public static final List<ProjectStatus> INVISIBLE_STATUSES = List.of(DELETED, HIDDEN);

    private final String displayName;

    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }
}