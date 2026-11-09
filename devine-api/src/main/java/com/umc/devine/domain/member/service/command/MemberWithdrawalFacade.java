package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.WithdrawalReqDTO;
import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.helper.EmailHasher;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 탈퇴 실행 진입점. 트랜잭션 전 준비(외부 호출)를 마친 뒤 한 트랜잭션으로 탈퇴를 실행한다(01-1 FUNC-MEM-003).
 * 외부 호출이 DB 행 잠금을 오래 잡지 않도록 트랜잭션 밖에서 한다.
 */
@Slf4j
@Component
public class MemberWithdrawalFacade {

    private final MemberWithdrawalCommandService commandService;
    private final ClerkApiClient clerkApiClient;
    private final EmailHasher emailHasher;
    private final boolean retainSanctionedMember;

    public MemberWithdrawalFacade(
            MemberWithdrawalCommandService commandService,
            ClerkApiClient clerkApiClient,
            EmailHasher emailHasher,
            @Value("${withdrawal.retain-sanctioned-member:false}") boolean retainSanctionedMember
    ) {
        this.commandService = commandService;
        this.clerkApiClient = clerkApiClient;
        this.emailHasher = emailHasher;
        this.retainSanctionedMember = retainSanctionedMember;
    }

    public WithdrawalResDTO.WithdrawResultDTO withdraw(Member member, WithdrawalReqDTO.WithdrawReq request) {
        return commandService.withdraw(member, request, prepare(member));
    }

    // 결정 C 대상이고 플래그가 켜져 있으면 Clerk에서 가입 이메일을 받아 해시를 만들어 둔다(FUNC-MEM-006 1번)
    private WithdrawalPreparation prepare(Member member) {
        if (!retainSanctionedMember || !member.isSanctioned()) {
            return WithdrawalPreparation.none();
        }
        try {
            Optional<String> hash = clerkApiClient.getPrimaryEmail(member.getClerkId()).flatMap(emailHasher::hash);
            if (hash.isEmpty()) {
                log.warn("[MemberWithdrawal] 결정 C 대상인데 가입 이메일이 없어 해시를 만들지 못함 - memberId: {}", member.getId());
                return new WithdrawalPreparation(null, true);
            }
            return new WithdrawalPreparation(hash.get(), false);
        } catch (Exception e) {
            // 탈퇴를 늦추지 않는다(약관 제7조 8항): 해시 없이 진행하고 운영자에게 알린다
            log.error("[MemberWithdrawal] 결정 C 이메일 조회 실패 - memberId: {}", member.getId(), e);
            return new WithdrawalPreparation(null, true);
        }
    }
}
