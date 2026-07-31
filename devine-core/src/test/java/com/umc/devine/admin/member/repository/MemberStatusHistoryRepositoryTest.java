package com.umc.devine.admin.member.repository;

import com.umc.devine.admin.member.entity.MemberStatusHistory;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemberStatusHistoryRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private MemberStatusHistoryRepository memberStatusHistoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;
    private Member admin;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .clerkId("clerk_member")
                .name("유저")
                .nickname("targetuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        admin = memberRepository.save(Member.builder()
                .clerkId("clerk_admin")
                .name("관리자")
                .nickname("admin")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    private MemberStatusHistory createHistory(MemberStatusAction action, MemberStatus status) {
        return memberStatusHistoryRepository.save(MemberStatusHistory.builder()
                .member(member)
                .action(action)
                .status(status)
                .reason("정책 위반")
                .notifyRequested(false)
                .processor(admin)
                .build());
    }

    @Nested
    @DisplayName("findByMemberIdOrderByCreatedAtDesc")
    class FindByMemberIdTest {

        @Test
        @DisplayName("이력을 최신순으로 조회한다")
        void findByMemberIdOrderByCreatedAtDesc_returnsDescOrder() {
            // given
            createHistory(MemberStatusAction.SUSPEND, MemberStatus.SUSPENDED);
            createHistory(MemberStatusAction.UNSUSPEND, MemberStatus.ACTIVE);

            // when
            List<MemberStatusHistory> result = memberStatusHistoryRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAction()).isEqualTo(MemberStatusAction.UNSUSPEND);
            assertThat(result.get(1).getAction()).isEqualTo(MemberStatusAction.SUSPEND);
        }

        @Test
        @DisplayName("이력이 없으면 빈 목록을 반환한다")
        void findByMemberIdOrderByCreatedAtDesc_empty() {
            // when
            List<MemberStatusHistory> result = memberStatusHistoryRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());

            // then
            assertThat(result).isEmpty();
        }
    }
}
