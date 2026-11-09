package com.umc.devine.domain.member;

import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintReasonCode;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.complaint.repository.ComplaintRepository;
import com.umc.devine.admin.member.entity.MemberStatusHistory;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.admin.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.member.dto.WithdrawalReqDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberRestriction;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorReason;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.MemberRestrictionRepository;
import com.umc.devine.domain.member.service.command.MemberCommandService;
import com.umc.devine.domain.member.service.command.MemberWithdrawalFacade;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.email.EmailNotificationEvent;
import com.umc.devine.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;

@RecordApplicationEvents
@TestPropertySource(properties = {
        "withdrawal.retain-sanctioned-member=true",
        "withdrawal.email-hash-pepper=test-pepper"
})
class SanctionedMemberRetentionTest extends IntegrationTestSupport {

    @Autowired private MemberWithdrawalFacade withdrawalFacade;
    @Autowired private MemberCommandService memberCommandService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberRestrictionRepository memberRestrictionRepository;
    @Autowired private MemberStatusHistoryRepository memberStatusHistoryRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private ApplicationEvents events;
    @Autowired private EntityManager em;

    @MockitoBean private ClerkApiClient clerkApiClient;

    private Member suspended;
    private Member reporter;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        reporter = memberRepository.save(Member.builder().clerkId("clerk_sr_reporter").nickname("srreporter")
                .mainType(MemberMainType.DEVELOPER).used(MemberStatus.ACTIVE).build());
        suspended = memberRepository.save(Member.builder().clerkId("clerk_sr_suspended").nickname("srsuspended")
                .mainType(MemberMainType.DEVELOPER).used(MemberStatus.ACTIVE).build());
        suspended.suspend();
        memberStatusHistoryRepository.save(MemberStatusHistory.builder().member(suspended)
                .action(MemberStatusAction.SUSPEND).status(MemberStatus.SUSPENDED).reason("욕설 신고 누적").build());
        complaint = complaintRepository.save(Complaint.builder().complainant(reporter).respondentMember(suspended)
                .targetType(ComplaintTargetType.DEVELOPER).targetId(suspended.getId()).reasonCode(ComplaintReasonCode.ABUSE).build());
        em.flush();
    }

    private static WithdrawalReqDTO.WithdrawReq confirmed() {
        return WithdrawalReqDTO.WithdrawReq.builder().confirmed(true).build();
    }

    @Test
    @DisplayName("T12: 정지 회원이 탈퇴하면 이메일 해시, 사유, 제재 요약을 1년 보관하고, 기존 이력과 신고 식별 칸을 남긴다")
    void retainsSanctionInfo() {
        given(clerkApiClient.getPrimaryEmail("clerk_sr_suspended")).willReturn(Optional.of("Banned@Example.com"));

        withdrawalFacade.withdraw(suspended, confirmed());
        em.flush();
        em.clear();

        List<MemberRestriction> restrictions = memberRestrictionRepository.findAll();
        assertThat(restrictions).singleElement().satisfies(r -> {
            assertThat(r.getMember().getId()).isEqualTo(suspended.getId());
            assertThat(r.getEmailHash()).hasSize(64).doesNotContain("@");
            assertThat(r.getReason()).isEqualTo("욕설 신고 누적");
            assertThat(r.getSanctionSummary()).contains("SUSPEND").contains("피신고: 1건");
            assertThat(r.getExpiresAt()).isCloseTo(LocalDateTime.now().plusYears(1), within(1, java.time.temporal.ChronoUnit.MINUTES));
        });
        assertThat(memberStatusHistoryRepository.findByMemberIdOrderByCreatedAtDesc(suspended.getId()))
                .extracting(MemberStatusHistory::getAction)
                .containsExactlyInAnyOrder(MemberStatusAction.SUSPEND, MemberStatusAction.SELF_WITHDRAW);
        assertThat(complaintRepository.findById(complaint.getId()).orElseThrow().getRespondentMember()).isNotNull();
        assertThat(memberRepository.findById(suspended.getId()).orElseThrow().getUsed()).isEqualTo(MemberStatus.DELETED);
    }

    @Test
    @DisplayName("T12: 같은 이메일로 재가입하면 막고 문의 안내를 준다(대소문자 무관)")
    void blocksResignup() {
        given(clerkApiClient.getPrimaryEmail("clerk_sr_suspended")).willReturn(Optional.of("Banned@Example.com"));
        withdrawalFacade.withdraw(suspended, confirmed());
        em.flush();

        ClerkPrincipal newAccount = new ClerkPrincipal("clerk_sr_new", "banned@example.com", "새계정", null);

        assertThatThrownBy(() -> memberCommandService.signup(newAccount, null))
                .isInstanceOf(MemberException.class)
                .extracting("reason").isEqualTo(MemberErrorReason.RESTRICTED_FROM_SIGNUP);
        assertThat(MemberErrorReason.RESTRICTED_FROM_SIGNUP.getMessage()).contains("projectdevine2025@gmail.com");
    }

    @Test
    @DisplayName("Clerk 조회가 실패해도 탈퇴는 막지 않는다: 일반 탈퇴로 처리하고 운영자 알림을 남긴다")
    void clerkFailure_doesNotBlockWithdrawal() {
        given(clerkApiClient.getPrimaryEmail("clerk_sr_suspended")).willThrow(new IllegalStateException("Clerk 장애"));

        withdrawalFacade.withdraw(suspended, confirmed());
        em.flush();
        em.clear();

        assertThat(memberRestrictionRepository.findAll()).isEmpty();
        assertThat(memberRepository.findById(suspended.getId()).orElseThrow().getUsed()).isEqualTo(MemberStatus.DELETED);
        assertThat(memberStatusHistoryRepository.findByMemberIdOrderByCreatedAtDesc(suspended.getId()))
                .extracting(MemberStatusHistory::getAction).containsExactly(MemberStatusAction.SELF_WITHDRAW);
        assertThat(complaintRepository.findById(complaint.getId()).orElseThrow().getRespondentMember()).isNull();
        assertThat(events.stream(EmailNotificationEvent.class))
                .anySatisfy(e -> assertThat(e.getSubject()).contains("재가입 제한 정보를 남기지 못했습니다"));
    }

    @Test
    @DisplayName("제재 중이 아닌 회원은 플래그가 켜져 있어도 Clerk를 부르지 않고 일반 탈퇴로 처리한다")
    void activeMemberNotRetained() {
        withdrawalFacade.withdraw(reporter, confirmed());
        em.flush();

        assertThat(memberRestrictionRepository.findAll()).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(clerkApiClient);
    }
}
