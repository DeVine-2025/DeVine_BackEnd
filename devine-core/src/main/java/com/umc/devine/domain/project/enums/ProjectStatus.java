package com.umc.devine.domain.project.enums;

import lombok.Getter;

/**
 * 프로젝트의 라이프사이클 상태.
 * 유저 화면 노출 여부는 이 상태와 분리된 {@code Project.hidden} 플래그가 담당한다.
 */
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