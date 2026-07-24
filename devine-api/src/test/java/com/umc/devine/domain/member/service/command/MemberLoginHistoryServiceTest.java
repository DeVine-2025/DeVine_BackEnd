package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberLoginHistory;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberLoginHistoryRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemberLoginHistoryServiceTest extends IntegrationTestSupport {

    @Autowired
    private MemberLoginHistoryService memberLoginHistoryService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberLoginHistoryRepository memberLoginHistoryRepository;

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

    @Nested
    @DisplayName("recordLoginIfNeeded")
    class RecordLoginIfNeededTest {

        @Test
        @DisplayName("최근 기록이 없으면 새 로그인 이력을 기록한다")
        void recordLoginIfNeeded_recordsWhenNoRecent() {
            // when
            memberLoginHistoryService.recordLoginIfNeeded(member.getId());

            // then
            List<MemberLoginHistory> histories = memberLoginHistoryRepository.findTop10ByMemberIdOrderByLoginAtDesc(member.getId());
            assertThat(histories).hasSize(1);
        }

        @Test
        @DisplayName("최근 10분 이내 기록이 있으면 중복 기록하지 않는다")
        void recordLoginIfNeeded_skipsWhenRecentExists() {
            // given
            memberLoginHistoryRepository.save(MemberLoginHistory.builder()
                    .member(member)
                    .loginAt(LocalDateTime.now().minusMinutes(1))
                    .build());

            // when
            memberLoginHistoryService.recordLoginIfNeeded(member.getId());

            // then
            List<MemberLoginHistory> histories = memberLoginHistoryRepository.findTop10ByMemberIdOrderByLoginAtDesc(member.getId());
            assertThat(histories).hasSize(1);
        }

        @Test
        @DisplayName("10분 이전 기록만 있으면 새로 기록한다")
        void recordLoginIfNeeded_recordsWhenOldRecordOnly() {
            // given
            memberLoginHistoryRepository.save(MemberLoginHistory.builder()
                    .member(member)
                    .loginAt(LocalDateTime.now().minusMinutes(20))
                    .build());

            // when
            memberLoginHistoryService.recordLoginIfNeeded(member.getId());

            // then
            List<MemberLoginHistory> histories = memberLoginHistoryRepository.findTop10ByMemberIdOrderByLoginAtDesc(member.getId());
            assertThat(histories).hasSize(2);
        }
    }
}
