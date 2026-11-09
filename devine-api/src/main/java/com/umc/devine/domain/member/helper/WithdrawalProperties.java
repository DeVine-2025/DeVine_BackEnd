package com.umc.devine.domain.member.helper;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 회원 탈퇴 설정(application.yml의 withdrawal.*).
 */
@Getter
@Component
public class WithdrawalProperties {

    /** 결정 C 플래그(withdrawal.retain-sanctioned-member). 기본값 false. */
    private final boolean retainSanctionedMember;

    /** 운영자 알림 메일 주소. */
    private final String operatorEmail;

    public WithdrawalProperties(
            @Value("${withdrawal.retain-sanctioned-member:false}") boolean retainSanctionedMember,
            @Value("${withdrawal.operator-email:projectdevine2025@gmail.com}") String operatorEmail
    ) {
        this.retainSanctionedMember = retainSanctionedMember;
        this.operatorEmail = operatorEmail;
    }
}
