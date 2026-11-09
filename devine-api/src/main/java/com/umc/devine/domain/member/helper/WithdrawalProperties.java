package com.umc.devine.domain.member.helper;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class WithdrawalProperties {

    /** 정지/소명 중 탈퇴자 재가입 제한. 개인정보처리방침 개정 전까지 false */
    private final boolean retainSanctionedMember;

    private final String operatorEmail;

    public WithdrawalProperties(
            @Value("${withdrawal.retain-sanctioned-member:false}") boolean retainSanctionedMember,
            @Value("${withdrawal.operator-email:projectdevine2025@gmail.com}") String operatorEmail
    ) {
        this.retainSanctionedMember = retainSanctionedMember;
        this.operatorEmail = operatorEmail;
    }
}
