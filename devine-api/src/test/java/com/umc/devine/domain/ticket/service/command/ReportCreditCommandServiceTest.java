package com.umc.devine.domain.ticket.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.exception.TicketException;
import com.umc.devine.domain.ticket.exception.code.TicketErrorReason;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReportCreditCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private ReportCreditCommandService reportCreditCommandService;

    @Autowired
    private MemberReportCreditRepository memberReportCreditRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .clerkId("test-clerk-id")
                .nickname("tester")
                .used(MemberStatus.ACTIVE)
                .mainType(MemberMainType.DEVELOPER)
                .build());
    }

    @AfterEach
    void tearDown() {
        memberReportCreditRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Nested
    @DisplayName("initializeCredit")
    class InitializeCredit {

        @Test
        @DisplayName("회원가입 시 초기 크레딧 행이 생성된다")
        void createsCreditRow() {
            reportCreditCommandService.initializeCredit(member);

            MemberReportCredit credit = memberReportCreditRepository.findByMember(member).orElseThrow();
            assertThat(credit.getRemainingCount()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("이미 행이 있으면 예외 없이 무시된다")
        void idempotent() {
            memberReportCreditRepository.save(MemberReportCredit.of(member, 1));

            // 예외 발생 없이 통과해야 한다
            reportCreditCommandService.initializeCredit(member);
        }
    }

    @Nested
    @DisplayName("useCreditAtomic")
    class UseCreditAtomic {

        @Test
        @DisplayName("크레딧이 있으면 1 차감된다")
        void deductsOneCredit() {
            memberReportCreditRepository.save(MemberReportCredit.of(member, 2));

            reportCreditCommandService.useCreditAtomic(member);

            MemberReportCredit credit = memberReportCreditRepository.findByMember(member).orElseThrow();
            assertThat(credit.getRemainingCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("크레딧이 0이면 INSUFFICIENT_CREDITS 예외가 발생한다")
        void throwsWhenNoCredits() {
            memberReportCreditRepository.save(MemberReportCredit.of(member, 0));

            assertThatThrownBy(() -> reportCreditCommandService.useCreditAtomic(member))
                    .isInstanceOf(TicketException.class)
                    .satisfies(e -> assertThat(((TicketException) e).getReason())
                            .isEqualTo(TicketErrorReason.INSUFFICIENT_CREDITS));
        }
    }

    @Nested
    @DisplayName("refundCredit")
    class RefundCredit {

        @Test
        @DisplayName("환불 시 크레딧이 1 증가한다")
        void addsOneCredit() {
            memberReportCreditRepository.save(MemberReportCredit.of(member, 0));

            reportCreditCommandService.refundCredit(member);

            MemberReportCredit credit = memberReportCreditRepository.findByMember(member).orElseThrow();
            assertThat(credit.getRemainingCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("크레딧 행이 없으면 예외 없이 누락 처리된다")
        void noExceptionWhenNoCreditRow() {
            // 행 없음 — 예외가 발생하지 않아야 한다
            reportCreditCommandService.refundCredit(member);

            // 행이 생성되지 않아야 한다
            assertThat(memberReportCreditRepository.findByMember(member)).isEmpty();
        }

        @Test
        @DisplayName("초기 크레딧보다 많이 보유한 경우에도 환불이 막히지 않고 1 증가한다")
        void refundsEvenWhenAboveInitialCount() {
            // given - 결제로 5개 보유 (기본 제공 크레딧 1개보다 많음)
            memberReportCreditRepository.save(MemberReportCredit.of(member, 5));

            // when - 실패 환불
            reportCreditCommandService.refundCredit(member);

            // then - 상한(initialCount)에 막히지 않고 6으로 증가해야 한다
            MemberReportCredit credit = memberReportCreditRepository.findByMember(member).orElseThrow();
            assertThat(credit.getRemainingCount()).isEqualTo(6);
        }
    }
}
