package com.umc.devine.domain.project.enums;

import lombok.Getter;

@Getter
public enum ProjectPart {

    BACKEND("백엔드"),
    FRONTEND("프론트엔드"),
    DESIGN("디자인"),
    PM("기획/PM"),
    IOS("iOS"),
    ANDROID("Android");

    private final String displayName;

    ProjectPart(String displayName) {
        this.displayName = displayName;
    }
}
