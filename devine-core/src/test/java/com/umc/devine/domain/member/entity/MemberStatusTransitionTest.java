package com.umc.devine.domain.member.entity;

import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberStatusTransitionTest {

    private Member createMember(MemberStatus status) {
        return Member.builder()
                .clerkId("clerk_1")
                .name("테스트")
                .nickname("tester")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(status)
                .build();
    }

    @Nested
    @DisplayName("suspend")
    class SuspendTest {

        @Test
        @DisplayName("정지 시 상태가 SUSPENDED로 변경된다")
        void suspend_changesStatus() {
            Member member = createMember(MemberStatus.ACTIVE);

            member.suspend();

            assertThat(member.getUsed()).isEqualTo(MemberStatus.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("unsuspend")
    class UnsuspendTest {

        @Test
        @DisplayName("정지 해제 시 상태가 ACTIVE로 변경된다")
        void unsuspend_changesStatus() {
            Member member = createMember(MemberStatus.SUSPENDED);

            member.unsuspend();

            assertThat(member.getUsed()).isEqualTo(MemberStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("scheduleForceWithdrawal")
    class ScheduleForceWithdrawalTest {

        @Test
        @DisplayName("강제탈퇴 예정 시 PENDING_WITHDRAWAL 상태와 예정일시가 설정된다")
        void scheduleForceWithdrawal_setsStatusAndScheduledAt() {
            Member member = createMember(MemberStatus.ACTIVE);
            LocalDateTime scheduledAt = LocalDateTime.now().plusDays(30);

            member.scheduleForceWithdrawal(scheduledAt);

            assertThat(member.getUsed()).isEqualTo(MemberStatus.PENDING_WITHDRAWAL);
            assertThat(member.getScheduledWithdrawalAt()).isEqualTo(scheduledAt);
        }
    }

    @Nested
    @DisplayName("cancelScheduledWithdrawal")
    class CancelScheduledWithdrawalTest {

        @Test
        @DisplayName("탈퇴 예정 취소 시 ACTIVE로 복귀하고 예정일시가 제거된다")
        void cancelScheduledWithdrawal_revertsToActive() {
            Member member = createMember(MemberStatus.PENDING_WITHDRAWAL);
            member.scheduleForceWithdrawal(LocalDateTime.now().plusDays(30));

            member.cancelScheduledWithdrawal();

            assertThat(member.getUsed()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getScheduledWithdrawalAt()).isNull();
        }
    }

    @Nested
    @DisplayName("finalizeWithdrawal")
    class FinalizeWithdrawalTest {

        @Test
        @DisplayName("탈퇴 최종 확정 시 DELETED 상태가 되고 예정일시가 제거된다")
        void finalizeWithdrawal_setsDeleted() {
            Member member = createMember(MemberStatus.PENDING_WITHDRAWAL);
            member.scheduleForceWithdrawal(LocalDateTime.now().plusDays(30));

            member.finalizeWithdrawal();

            assertThat(member.getUsed()).isEqualTo(MemberStatus.DELETED);
            assertThat(member.getScheduledWithdrawalAt()).isNull();
        }
    }
}
