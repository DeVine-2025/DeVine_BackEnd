package com.umc.devine.domain.project.enums;

import lombok.Getter;

@Getter
public enum ProjectMode {

    ONLINE("온라인"),
    OFFLINE("오프라인"),
    HYBRID("온라인/오프라인");   // 온오프라인 병행

    private final String displayName;

    ProjectMode(String displayName) {
        this.displayName = displayName;
    }
}
