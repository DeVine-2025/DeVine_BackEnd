package com.umc.devine.domain.member.service.command;

public record WithdrawalPreparation(String emailHash, boolean emailHashFailed) {

    public static WithdrawalPreparation none() {
        return new WithdrawalPreparation(null, false);
    }
}
