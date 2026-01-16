package com.umc.devine.domain.project.enums;

import lombok.Getter;

@Getter
public enum ProjectField {

    WEB("웹"),
    MOBILE("모바일/앱"),
    AI("AI/머신러닝"),
    GAME("게임"),
    DATA("데이터"),
    BACKEND("백엔드"),
    FRONTEND("프론트엔드");

    private final String displayName;

    ProjectField(String displayName) {
        this.displayName = displayName;
    }
}
