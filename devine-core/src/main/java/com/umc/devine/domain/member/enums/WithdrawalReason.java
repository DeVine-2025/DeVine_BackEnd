package com.umc.devine.domain.member.enums;

import lombok.Getter;

@Getter
public enum WithdrawalReason {
    NO_USEFUL_FEATURE("쓸 기능이 없어요"),
    MATCHING_UNSATISFIED("원하는 매칭이 안 돼요"),
    REPORT_UNSATISFIED("리포트가 기대와 달라요"),
    USING_OTHER_SERVICE("다른 서비스를 써요"),
    OTHER("기타");

    private final String displayName;

    WithdrawalReason(String displayName) {
        this.displayName = displayName;
    }
}
