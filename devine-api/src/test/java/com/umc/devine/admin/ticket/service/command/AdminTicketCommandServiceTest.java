package com.umc.devine.admin.ticket.service.command;

import com.umc.devine.admin.ticket.dto.AdminTicketResDTO;
import com.umc.devine.admin.ticket.exception.AdminTicketException;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.ticket.entity.CreditRefundRequest;
import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import com.umc.devine.domain.ticket.repository.CreditRefundRequestRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminTicketCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private AdminTicketCommandService adminTicketCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CreditRefundRequestRepository creditRefundRequestRepository;

    private Member requester;
    private Member admin;

    @BeforeEach
    void setUp() {
        requester = memberRepository.save(Member.builder()
                .clerkId("clerk_requester")
                .name("신청자")
                .nickname("refundrequester")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.DELETED)
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

    @Nested
    @DisplayName("processRefundRequest")
    class ProcessRefundRequestTest {

        @Test
        @DisplayName("존재하지 않는 환불 신청ID면 예외가 발생한다")
        void processRefundRequest_notFound() {
            assertThatThrownBy(() -> adminTicketCommandService.processRefundRequest(999999L, admin.getClerkId()))
                    .isInstanceOf(AdminTicketException.class);
        }

        @Test
        @DisplayName("정상적으로 처리완료 상태로 전환된다")
        void processRefundRequest_success() {
            // given
            CreditRefundRequest refundRequest = creditRefundRequestRepository.save(CreditRefundRequest.of(requester, 3));

            // when
            AdminTicketResDTO.ProcessRefundRes result = adminTicketCommandService.processRefundRequest(refundRequest.getId(), admin.getClerkId());

            // then
            assertThat(result.status()).isEqualTo(CreditRefundStatus.PROCESSED);
            assertThat(result.processedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 처리완료된 건을 다시 처리하면 예외가 발생한다")
        void processRefundRequest_alreadyProcessed() {
            // given
            CreditRefundRequest refundRequest = creditRefundRequestRepository.save(CreditRefundRequest.of(requester, 3));
            adminTicketCommandService.processRefundRequest(refundRequest.getId(), admin.getClerkId());

            // when & then
            assertThatThrownBy(() -> adminTicketCommandService.processRefundRequest(refundRequest.getId(), admin.getClerkId()))
                    .isInstanceOf(AdminTicketException.class);
        }
    }
}
