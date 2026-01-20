package com.umc.devine.domain.category.enums;

public enum CategoryGenre {
    HEALTHCARE("헬스케어"),
    ECOMMERCE("이커머스"),
    FINANCE("금융/핀테크"),
    EDUCATION("교육/에듀테크"),
    ENTERTAINMENT("엔터테인먼트"),
    ETC("기타");

    private final String displayName;

    CategoryGenre(String displayName) {  // 생성자명 수정
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
