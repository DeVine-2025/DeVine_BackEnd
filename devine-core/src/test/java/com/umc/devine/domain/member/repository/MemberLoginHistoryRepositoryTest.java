package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberLoginHistory;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemberLoginHistoryRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private MemberLoginHistoryRepository memberLoginHistoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

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
    }

    private MemberLoginHistory createLogin(LocalDateTime loginAt) {
        return memberLoginHistoryRepository.save(MemberLoginHistory.builder()
                .member(member)
                .loginAt(loginAt)
                .build());
    }

    @Nested
    @DisplayName("findTop10ByMemberIdOrderByLoginAtDesc")
    class FindTop10Test {

        @Test
        @DisplayName("최근 로그인 이력을 최신순으로 조회한다")
        void findTop10_returnsDescOrder() {
            // given
            createLogin(LocalDateTime.now().minusDays(1));
            createLogin(LocalDateTime.now());

            // when
            List<MemberLoginHistory> result = memberLoginHistoryRepository.findTop10ByMemberIdOrderByLoginAtDesc(member.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getLoginAt()).isAfter(result.get(1).getLoginAt());
        }
    }

    @Nested
    @DisplayName("existsByMemberIdAndLoginAtAfter")
    class ExistsAfterTest {

        @Test
        @DisplayName("기준 시각 이후 로그인 기록이 있으면 true를 반환한다")
        void existsAfter_true() {
            // given
            createLogin(LocalDateTime.now());

            // when
            boolean result = memberLoginHistoryRepository.existsByMemberIdAndLoginAtAfter(member.getId(), LocalDateTime.now().minusMinutes(10));

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("기준 시각 이후 로그인 기록이 없으면 false를 반환한다")
        void existsAfter_false() {
            // given
            createLogin(LocalDateTime.now().minusHours(1));

            // when
            boolean result = memberLoginHistoryRepository.existsByMemberIdAndLoginAtAfter(member.getId(), LocalDateTime.now().minusMinutes(10));

            // then
            assertThat(result).isFalse();
        }
    }
}
