package com.umc.devine.domain.member.service.command;

/** emailHash는 정지/소명 중 탈퇴자 재가입 제한용이고, 만들지 못하면 null이다. */
public record WithdrawalPreparation(String emailHash, boolean emailHashFailed) {

    public static WithdrawalPreparation none() {
        return new WithdrawalPreparation(null, false);
    }
}
