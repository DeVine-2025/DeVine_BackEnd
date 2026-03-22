package com.umc.devine.domain.project.enums;

import lombok.Getter;

@Getter
public enum ProjectPart {

    PM("PM"),
    FRONTEND("프론트엔드"),
    BACKEND("백엔드"),
    INFRA("인프라");

    private final String displayName;

    ProjectPart(String displayName) {
        this.displayName = displayName;
    }
}
