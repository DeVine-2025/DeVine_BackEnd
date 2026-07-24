package com.umc.devine.admin.member.service.command;

import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.dto.AdminMemberResDTO;
import com.umc.devine.admin.member.entity.MemberStatusHistory;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.admin.member.exception.MemberAdminException;
import com.umc.devine.admin.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminMemberCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private AdminMemberCommandService adminMemberCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberStatusHistoryRepository memberStatusHistoryRepository;

    private Member target;
    private Member admin;

    @BeforeEach
    void setUp() {
        target = memberRepository.save(Member.builder()
                .clerkId("clerk_target")
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

    private void setStatus(MemberStatus status) {
        target = memberRepository.findById(target.getId()).orElseThrow();
        switch (status) {
            case SUSPENDED -> target.suspend();
            case PENDING_WITHDRAWAL -> target.scheduleForceWithdrawal(LocalDateTime.now().plusDays(30));
            case DELETED -> target.withdraw();
            default -> {}
        }
        memberRepository.save(target);
    }

    @Nested
    @DisplayName("changeStatus - 존재하지 않는 유저")
    class NotFoundTest {

        @Test
        @DisplayName("존재하지 않는 유저ID면 예외가 발생한다")
        void changeStatus_notFound() {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.SUSPEND)
                    .reason("정책 위반")
                    .build();

            assertThatThrownBy(() -> adminMemberCommandService.changeStatus("no-such-nickname", admin.getId(), request))
                    .isInstanceOf(MemberAdminException.class);
        }
    }

    @Nested
    @DisplayName("changeStatus - SUSPEND")
    class SuspendTest {

        @Test
        @DisplayName("ACTIVE 계정을 정지시키면 SUSPENDED로 변경된다")
        void suspend_success() {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.SUSPEND)
                    .reason("커뮤니티 이용규칙 위반")
                    .notifyRequested(true)
                    .build();

            AdminMemberResDTO.ChangeStatusRes result = adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request);

            assertThat(result.status()).isEqualTo(MemberStatus.SUSPENDED);
        }

        @Test
        @DisplayName("사유 없이 정지 요청하면 예외가 발생한다")
        void suspend_withoutReason() {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.SUSPEND)
                    .build();

            assertThatThrownBy(() -> adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request))
                    .isInstanceOf(MemberAdminException.class);
        }

        @Test
        @DisplayName("이미 탈퇴한 계정은 정지할 수 없다")
        void suspend_alreadyWithdrawn() {
            setStatus(MemberStatus.DELETED);
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.SUSPEND)
                    .reason("커뮤니티 이용규칙 위반")
                    .build();

            assertThatThrownBy(() -> adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request))
                    .isInstanceOf(MemberAdminException.class);
        }

        @Test
        @DisplayName("상태 변경 시 이력이 기록된다")
        void suspend_recordsHistory() {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.SUSPEND)
                    .reason("커뮤니티 이용규칙 위반")
                    .build();

            adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request);

            List<MemberStatusHistory> histories = memberStatusHistoryRepository.findByMemberIdOrderByCreatedAtDesc(target.getId());
            assertThat(histories).hasSize(1);
            assertThat(histories.get(0).getAction()).isEqualTo(MemberStatusAction.SUSPEND);
            assertThat(histories.get(0).getProcessor().getId()).isEqualTo(admin.getId());
        }
    }

    @Nested
    @DisplayName("changeStatus - UNSUSPEND")
    class UnsuspendTest {

        @Test
        @DisplayName("SUSPENDED 계정을 해제하면 ACTIVE로 변경된다")
        void unsuspend_success() {
            setStatus(MemberStatus.SUSPENDED);
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.UNSUSPEND)
                    .build();

            AdminMemberResDTO.ChangeStatusRes result = adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request);

            assertThat(result.status()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("정지 상태가 아닌 계정은 해제할 수 없다")
        void unsuspend_notSuspended() {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.UNSUSPEND)
                    .build();

            assertThatThrownBy(() -> adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request))
                    .isInstanceOf(MemberAdminException.class);
        }
    }

    @Nested
    @DisplayName("changeStatus - FORCE_WITHDRAW")
    class ForceWithdrawTest {

        @Test
        @DisplayName("강제탈퇴 요청 시 PENDING_WITHDRAWAL로 변경되고 30일 후로 예정일시가 설정된다")
        void forceWithdraw_success() {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.FORCE_WITHDRAW)
                    .reason("중대한 규정 위반")
                    .build();

            AdminMemberResDTO.ChangeStatusRes result = adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request);

            assertThat(result.status()).isEqualTo(MemberStatus.PENDING_WITHDRAWAL);
            assertThat(result.scheduledWithdrawalAt()).isAfter(LocalDateTime.now().plusDays(29));
            assertThat(result.scheduledWithdrawalAt()).isBefore(LocalDateTime.now().plusDays(31));
        }

        @Test
        @DisplayName("사유 없이 강제탈퇴 요청하면 예외가 발생한다")
        void forceWithdraw_withoutReason() {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.FORCE_WITHDRAW)
                    .build();

            assertThatThrownBy(() -> adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request))
                    .isInstanceOf(MemberAdminException.class);
        }

        @Test
        @DisplayName("이미 탈퇴한 계정은 강제탈퇴할 수 없다")
        void forceWithdraw_alreadyWithdrawn() {
            setStatus(MemberStatus.DELETED);
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.FORCE_WITHDRAW)
                    .reason("중대한 규정 위반")
                    .build();

            assertThatThrownBy(() -> adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request))
                    .isInstanceOf(MemberAdminException.class);
        }
    }

    @Nested
    @DisplayName("changeStatus - CANCEL_WITHDRAWAL")
    class CancelWithdrawalTest {

        @Test
        @DisplayName("소명 성공 시 강제탈퇴 예정을 취소하고 ACTIVE로 복귀한다")
        void cancelWithdrawal_success() {
            setStatus(MemberStatus.PENDING_WITHDRAWAL);
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.CANCEL_WITHDRAWAL)
                    .reason("소명 완료")
                    .build();

            AdminMemberResDTO.ChangeStatusRes result = adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request);

            assertThat(result.status()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(result.scheduledWithdrawalAt()).isNull();
        }

        @Test
        @DisplayName("강제탈퇴 예정 상태가 아니면 취소할 수 없다")
        void cancelWithdrawal_notPending() {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.CANCEL_WITHDRAWAL)
                    .build();

            assertThatThrownBy(() -> adminMemberCommandService.changeStatus(target.getNickname(), admin.getId(), request))
                    .isInstanceOf(MemberAdminException.class);
        }
    }
}
