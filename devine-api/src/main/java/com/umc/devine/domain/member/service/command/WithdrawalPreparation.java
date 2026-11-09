package com.umc.devine.domain.member.service.command;

/**
 * 탈퇴 트랜잭션 전에 외부 호출로 준비한 값(01-1 FUNC-MEM-003 2번). DB는 바꾸지 않는다.
 *
 * @param emailHash         결정 C 대상·플래그 ON일 때 만든 가입 이메일 해시. 없으면 null
 * @param emailHashFailed   결정 C 대상·플래그 ON인데 Clerk 조회·해시에 실패했는지. 실패해도 탈퇴는 막지 않는다
 */
public record WithdrawalPreparation(String emailHash, boolean emailHashFailed) {

    public static WithdrawalPreparation none() {
        return new WithdrawalPreparation(null, false);
    }
}
