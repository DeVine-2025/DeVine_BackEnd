package com.umc.devine.global.scheduler;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberWithdrawalFinalizeSchedulerTest extends IntegrationTestSupport {

    @Autowired
    private MemberWithdrawalFinalizeScheduler scheduler;

    @Autowired
    private MemberRepository memberRepository;

    private Member expiredMember;
    private Member notYetDueMember;

    @BeforeEach
    void setUp() {
        expiredMember = memberRepository.save(Member.builder()
                .clerkId("clerk_expired")
                .name("만료유저")
                .nickname("expireduser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
        expiredMember.scheduleForceWithdrawal(LocalDateTime.now().minusDays(1));
        memberRepository.save(expiredMember);

        notYetDueMember = memberRepository.save(Member.builder()
                .clerkId("clerk_pending")
                .name("대기유저")
                .nickname("pendinguser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
        notYetDueMember.scheduleForceWithdrawal(LocalDateTime.now().plusDays(10));
        memberRepository.save(notYetDueMember);
    }

    @Test
    @DisplayName("예정일시가 지난 계정만 DELETED로 최종 확정한다")
    void finalizeExpiredWithdrawals_finalizesOnlyExpired() {
        // when
        scheduler.finalizeExpiredWithdrawals();

        // then
        Member expired = memberRepository.findById(expiredMember.getId()).orElseThrow();
        Member notYetDue = memberRepository.findById(notYetDueMember.getId()).orElseThrow();

        assertThat(expired.getUsed()).isEqualTo(MemberStatus.DELETED);
        assertThat(expired.getScheduledWithdrawalAt()).isNull();
        assertThat(notYetDue.getUsed()).isEqualTo(MemberStatus.PENDING_WITHDRAWAL);
    }
}
