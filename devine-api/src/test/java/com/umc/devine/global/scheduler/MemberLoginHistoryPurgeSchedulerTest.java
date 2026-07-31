package com.umc.devine.global.scheduler;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberLoginHistory;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberLoginHistoryRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberLoginHistoryPurgeSchedulerTest extends IntegrationTestSupport {

    @Autowired
    private MemberLoginHistoryPurgeScheduler scheduler;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberLoginHistoryRepository memberLoginHistoryRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .clerkId("clerk_login_history_purge")
                .nickname("loginpurgeuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("3개월이 지난 로그인 이력은 탈퇴 여부와 무관하게(활성 회원이어도) 파기된다")
    void purgeExpiredLoginHistory_deletesOldHistoryRegardlessOfMemberStatus() {
        // given
        MemberLoginHistory old = memberLoginHistoryRepository.save(MemberLoginHistory.builder()
                .member(member)
                .loginAt(LocalDateTime.now().minusMonths(3).minusDays(1))
                .build());
        MemberLoginHistory recent = memberLoginHistoryRepository.save(MemberLoginHistory.builder()
                .member(member)
                .loginAt(LocalDateTime.now().minusDays(1))
                .build());

        // when
        scheduler.purgeExpiredLoginHistory();

        // then
        assertThat(memberLoginHistoryRepository.findById(old.getId())).isEmpty();
        assertThat(memberLoginHistoryRepository.findById(recent.getId())).isPresent();
        assertThat(memberRepository.findById(member.getId())).isPresent();
    }
}
