package com.umc.devine.domain.category.enums;

public enum CategoryGenre {
    HEALTHCARE("헬스케어"),
    FINTECH("핀테크"),
    ECOMMERCE("이커머스"),
    EDUCATION("교육"),
    SOCIAL("소셜/커뮤니티"),
    ENTERTAINMENT("엔터테인먼트"),
    AI_DATA("AI/데이터"),
    ETC("기타");

    private final String displayName;

    CategoryGenre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
