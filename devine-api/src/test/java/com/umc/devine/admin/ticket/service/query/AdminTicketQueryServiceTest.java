package com.umc.devine.admin.ticket.service.query;

import com.umc.devine.admin.ticket.dto.AdminTicketReqDTO;
import com.umc.devine.admin.ticket.dto.AdminTicketResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.ticket.entity.CreditRefundRequest;
import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import com.umc.devine.domain.ticket.repository.CreditRefundRequestRepository;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AdminTicketQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private AdminTicketQueryService adminTicketQueryService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CreditRefundRequestRepository creditRefundRequestRepository;

    private Member requester;

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
    }

    @Nested
    @DisplayName("getRefundRequests")
    class GetRefundRequestsTest {

        @Test
        @DisplayName("상태 필터 없이 전체 목록을 조회한다")
        void getRefundRequests_noFilter() {
            // given
            creditRefundRequestRepository.save(CreditRefundRequest.of(requester, 2));
            AdminTicketReqDTO.RefundRequestSearchReq request = AdminTicketReqDTO.RefundRequestSearchReq.builder().build();

            // when
            PagedResponse<AdminTicketResDTO.RefundRequestDTO> result = adminTicketQueryService.getRefundRequests(request);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).creditAmountAtRequest()).isEqualTo(2);
        }

        @Test
        @DisplayName("상태 필터로 조회하면 해당 상태의 신청만 반환한다")
        void getRefundRequests_byStatus() {
            // given
            CreditRefundRequest processed = creditRefundRequestRepository.save(CreditRefundRequest.of(requester, 1));
            processed.process(null);
            creditRefundRequestRepository.save(processed);
            creditRefundRequestRepository.save(CreditRefundRequest.of(requester, 5));

            AdminTicketReqDTO.RefundRequestSearchReq request = AdminTicketReqDTO.RefundRequestSearchReq.builder()
                    .status(CreditRefundStatus.REQUESTED)
                    .build();

            // when
            PagedResponse<AdminTicketResDTO.RefundRequestDTO> result = adminTicketQueryService.getRefundRequests(request);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).creditAmountAtRequest()).isEqualTo(5);
        }
    }
}
