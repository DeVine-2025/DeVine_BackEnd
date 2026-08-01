package com.umc.devine.global.scheduler;

import com.umc.devine.admin.member.entity.MemberStatusHistory;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.admin.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 강제탈퇴(자격상실) 30일 소명 절차가 만료된 계정을 최종 탈퇴 확정 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberWithdrawalFinalizeScheduler {

    private final MemberRepository memberRepository;
    private final MemberStatusHistoryRepository memberStatusHistoryRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void finalizeExpiredWithdrawals() {
        List<Member> expired = memberRepository.findByUsedAndScheduledWithdrawalAtBefore(
                MemberStatus.PENDING_WITHDRAWAL, LocalDateTime.now());

        if (expired.isEmpty()) {
            log.info("[MemberWithdrawalFinalize] 확정할 강제탈퇴 예정 계정 없음");
            return;
        }

        expired.forEach(member -> {
            member.finalizeWithdrawal();
            memberStatusHistoryRepository.save(MemberStatusHistory.builder()
                    .member(member)
                    .action(MemberStatusAction.WITHDRAWAL_FINALIZED)
                    .status(member.getUsed())
                    .build());
        });
        log.info("[MemberWithdrawalFinalize] 강제탈퇴 최종 확정 완료 - {}건", expired.size());
    }
}
