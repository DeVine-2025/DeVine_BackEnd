package com.umc.devine.admin.member.service.query;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.dto.AdminMemberResDTO;
import com.umc.devine.admin.member.exception.MemberAdminException;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminMemberQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private AdminMemberQueryService adminMemberQueryService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .clerkId("clerk_member")
                .name("홍길동")
                .nickname("developer1")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        contactRepository.save(Contact.builder()
                .contactType(ContactType.EMAIL)
                .value("developer1@example.com")
                .member(member)
                .build());
    }

    @Nested
    @DisplayName("getMemberList")
    class GetMemberListTest {

        @Test
        @DisplayName("검색어 없이 전체 유저 목록을 조회한다")
        void getMemberList_noKeyword() {
            // given
            AdminMemberReqDTO.SearchReq request = AdminMemberReqDTO.SearchReq.builder().build();

            // when
            PagedResponse<AdminMemberResDTO.MemberSummaryDTO> result = adminMemberQueryService.getMemberList(request);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).email()).isEqualTo("developer1@example.com");
        }

        @Test
        @DisplayName("닉네임 검색어로 유저 목록을 조회한다")
        void getMemberList_byKeyword() {
            // given
            AdminMemberReqDTO.SearchReq request = AdminMemberReqDTO.SearchReq.builder().keyword("developer1").build();

            // when
            PagedResponse<AdminMemberResDTO.MemberSummaryDTO> result = adminMemberQueryService.getMemberList(request);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("이름 검색어로 유저 목록을 조회한다")
        void getMemberList_byName() {
            // given
            AdminMemberReqDTO.SearchReq request = AdminMemberReqDTO.SearchReq.builder().keyword("홍길동").build();

            // when
            PagedResponse<AdminMemberResDTO.MemberSummaryDTO> result = adminMemberQueryService.getMemberList(request);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("이메일 검색어로 유저 목록을 조회한다")
        void getMemberList_byEmail() {
            // given
            AdminMemberReqDTO.SearchReq request = AdminMemberReqDTO.SearchReq.builder().keyword("developer1@example.com").build();

            // when
            PagedResponse<AdminMemberResDTO.MemberSummaryDTO> result = adminMemberQueryService.getMemberList(request);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("페이지 크기만큼만 반환하고 전체 개수를 정확히 집계한다")
        void getMemberList_pagination() {
            // given
            for (int i = 0; i < 5; i++) {
                memberRepository.save(Member.builder()
                        .clerkId("clerk_extra_" + i)
                        .name("유저" + i)
                        .nickname("extra" + i)
                        .mainType(MemberMainType.DEVELOPER)
                        .disclosure(true)
                        .used(MemberStatus.ACTIVE)
                        .build());
            }
            AdminMemberReqDTO.SearchReq request = AdminMemberReqDTO.SearchReq.builder().page(1).size(2).build();

            // when
            PagedResponse<AdminMemberResDTO.MemberSummaryDTO> result = adminMemberQueryService.getMemberList(request);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(6);
        }

        @Test
        @DisplayName("일치하는 유저가 없으면 빈 목록을 반환한다")
        void getMemberList_noMatch() {
            // given
            AdminMemberReqDTO.SearchReq request = AdminMemberReqDTO.SearchReq.builder().keyword("nonexistent").build();

            // when
            PagedResponse<AdminMemberResDTO.MemberSummaryDTO> result = adminMemberQueryService.getMemberList(request);

            // then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMemberDetail")
    class GetMemberDetailTest {

        @Test
        @DisplayName("존재하지 않는 유저ID면 예외가 발생한다")
        void getMemberDetail_notFound() {
            assertThatThrownBy(() -> adminMemberQueryService.getMemberDetail("no-such-nickname"))
                    .isInstanceOf(MemberAdminException.class);
        }

        @Test
        @DisplayName("유저 상세를 조회한다")
        void getMemberDetail_success() {
            // when
            AdminMemberResDTO.MemberDetailRes result = adminMemberQueryService.getMemberDetail(member.getNickname());

            // then
            assertThat(result.nickname()).isEqualTo(member.getNickname());
            assertThat(result.email()).isEqualTo("developer1@example.com");
            assertThat(result.status()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(result.paymentSummary().totalCount()).isZero();
            assertThat(result.loginHistory()).isEmpty();
        }

        @Test
        @DisplayName("결제 이력이 있으면 결제 요약에 반영된다")
        void getMemberDetail_withPayments() {
            // given
            paymentRepository.save(Payment.builder()
                    .portonePaymentId("payment_1")
                    .member(member)
                    .orderName("리포트 열람권 1개")
                    .amount(5000L)
                    .currency("KRW")
                    .build());

            // when
            AdminMemberResDTO.MemberDetailRes result = adminMemberQueryService.getMemberDetail(member.getNickname());

            // then
            assertThat(result.paymentSummary().totalCount()).isEqualTo(1);
            assertThat(result.paymentSummary().totalAmount()).isEqualTo(5000L);
            assertThat(result.paymentSummary().recentPayments()).hasSize(1);
        }

        @Test
        @DisplayName("신고 이력이 없으면 신고 이력이 빈 목록으로 반환된다")
        void getMemberDetail_noComplaintHistory() {
            // when
            AdminMemberResDTO.MemberDetailRes result = adminMemberQueryService.getMemberDetail(member.getNickname());

            // then
            assertThat(result.respondentHistory().respondentComplaintCount()).isZero();
            assertThat(result.respondentHistory().respondentHistory()).isEmpty();
        }

        @Test
        @DisplayName("피신고 이력이 있으면 유저 상세에 누적 신고 건수와 이력이 반영된다")
        void getMemberDetail_withComplaintHistory() {
            // given
            Member complainant = memberRepository.save(Member.builder()
                    .clerkId("clerk_complainant")
                    .name("신고자")
                    .nickname("complainant")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());
            complaintRepository.save(Complaint.builder()
                    .complainant(complainant)
                    .respondentMember(member)
                    .targetType(com.umc.devine.admin.complaint.enums.ComplaintTargetType.CHAT)
                    .targetId(1L)
                    .reason("부적절한 콘텐츠입니다.")
                    .status(com.umc.devine.admin.complaint.enums.ComplaintStatus.PENDING)
                    .build());

            // when
            AdminMemberResDTO.MemberDetailRes result = adminMemberQueryService.getMemberDetail(member.getNickname());

            // then
            assertThat(result.respondentHistory().respondentComplaintCount()).isEqualTo(1);
            assertThat(result.respondentHistory().respondentHistory()).hasSize(1);
        }
    }
}
