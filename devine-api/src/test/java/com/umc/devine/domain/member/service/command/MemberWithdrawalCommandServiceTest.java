package com.umc.devine.domain.member.service.command;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.entity.MemberCoupon;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberStatusHistory;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.enums.MemberStatusAction;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import com.umc.devine.domain.ticket.repository.CreditRefundRequestRepository;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
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

class MemberWithdrawalCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private MemberWithdrawalCommandService memberWithdrawalCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private MemberReportCreditRepository memberReportCreditRepository;

    @Autowired
    private CreditRefundRequestRepository creditRefundRequestRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private MemberStatusHistoryRepository memberStatusHistoryRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .clerkId("clerk_withdraw_target")
                .name("탈퇴예정자")
                .nickname("withdrawtarget")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        contactRepository.save(Contact.builder()
                .contactType(ContactType.EMAIL)
                .value("withdrawtarget@example.com")
                .member(member)
                .build());
    }

    @Nested
    @DisplayName("selfWithdraw")
    class SelfWithdrawTest {

        @Test
        @DisplayName("확인 문구가 닉네임과 다르면 예외가 발생한다")
        void selfWithdraw_confirmationMismatch() {
            // given
            MemberReqDTO.SelfWithdrawReq request = MemberReqDTO.SelfWithdrawReq.builder()
                    .confirmationText("wrong-nickname")
                    .build();

            // when & then
            assertThatThrownBy(() -> memberWithdrawalCommandService.selfWithdraw(member, request))
                    .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("ACTIVE/INACTIVE가 아니면 예외가 발생한다")
        void selfWithdraw_invalidStatus() {
            // given
            member = memberRepository.save(Member.builder()
                    .clerkId("clerk_suspended")
                    .name("정지자")
                    .nickname("suspendeduser")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.SUSPENDED)
                    .build());
            MemberReqDTO.SelfWithdrawReq request = MemberReqDTO.SelfWithdrawReq.builder()
                    .confirmationText("suspendeduser")
                    .build();

            // when & then
            assertThatThrownBy(() -> memberWithdrawalCommandService.selfWithdraw(member, request))
                    .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("정상 탈퇴 시 계정이 DELETED로 전환되고 PII가 익명화된다")
        void selfWithdraw_success() {
            // given
            String originalNickname = member.getNickname();
            MemberReqDTO.SelfWithdrawReq request = MemberReqDTO.SelfWithdrawReq.builder()
                    .confirmationText(originalNickname)
                    .build();

            // when
            MemberResDTO.WithdrawalResultDTO result = memberWithdrawalCommandService.selfWithdraw(member, request);

            // then
            assertThat(result.withdrawn()).isTrue();
            Member updated = memberRepository.findById(member.getId()).orElseThrow();
            assertThat(updated.getUsed()).isEqualTo(MemberStatus.DELETED);
            assertThat(updated.getNickname()).isNotEqualTo(originalNickname);
            assertThat(updated.getName()).isNull();

            List<MemberStatusHistory> histories = memberStatusHistoryRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());
            assertThat(histories).hasSize(1);
            assertThat(histories.get(0).getAction()).isEqualTo(MemberStatusAction.SELF_WITHDRAW);
        }

        @Test
        @DisplayName("이미 탈퇴 처리된 계정에 다시 탈퇴를 요청하면 예외가 발생한다 (중복 클릭/경쟁 조건 방지)")
        void selfWithdraw_alreadyWithdrawn_throwsOnSecondAttempt() {
            // given
            MemberReqDTO.SelfWithdrawReq request = MemberReqDTO.SelfWithdrawReq.builder()
                    .confirmationText(member.getNickname())
                    .build();
            memberWithdrawalCommandService.selfWithdraw(member, request);

            // when & then — 두 번째 요청은 이미 DELETED로 바뀐 상태를 잠금 조회로 다시 확인하고 거부한다
            assertThatThrownBy(() -> memberWithdrawalCommandService.selfWithdraw(member, request))
                    .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("잔여 생성권이 있고 환불 신청 시 환불 신청 레코드가 생성되고 생성권은 소멸된다")
        void selfWithdraw_withRefundRequest_createsRefundRequestAndVoidsCredits() {
            // given
            memberReportCreditRepository.save(MemberReportCredit.of(member, 3));
            MemberReqDTO.SelfWithdrawReq request = MemberReqDTO.SelfWithdrawReq.builder()
                    .confirmationText(member.getNickname())
                    .refundRequested(true)
                    .build();

            // when
            MemberResDTO.WithdrawalResultDTO result = memberWithdrawalCommandService.selfWithdraw(member, request);

            // then
            assertThat(result.refundRequestCreated()).isTrue();
            assertThat(result.creditsForfeitedOrRefunded()).isEqualTo(3);
            assertThat(creditRefundRequestRepository.findAll())
                    .anySatisfy(refund -> {
                        assertThat(refund.getCreditAmountAtRequest()).isEqualTo(3);
                        assertThat(refund.getStatus()).isEqualTo(CreditRefundStatus.REQUESTED);
                    });
            MemberReportCredit credit = memberReportCreditRepository.findByMember(member).orElseThrow();
            assertThat(credit.getRemainingCount()).isZero();
        }

        @Test
        @DisplayName("잔여 생성권이 있어도 환불 미신청이면 환불 레코드 없이 소멸만 된다")
        void selfWithdraw_withoutRefundRequest_voidsCreditsOnly() {
            // given
            memberReportCreditRepository.save(MemberReportCredit.of(member, 2));
            MemberReqDTO.SelfWithdrawReq request = MemberReqDTO.SelfWithdrawReq.builder()
                    .confirmationText(member.getNickname())
                    .refundRequested(false)
                    .build();

            // when
            MemberResDTO.WithdrawalResultDTO result = memberWithdrawalCommandService.selfWithdraw(member, request);

            // then
            assertThat(result.refundRequestCreated()).isFalse();
            assertThat(creditRefundRequestRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("보유 쿠폰은 탈퇴 시 전량 소멸된다")
        void selfWithdraw_deletesCoupons() {
            // given
            Coupon coupon = couponRepository.save(Coupon.builder()
                    .name("웰컴 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(1000L)
                    .issuedCount(1)
                    .usedCount(0)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .isActive(true)
                    .build());
            memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));
            MemberReqDTO.SelfWithdrawReq request = MemberReqDTO.SelfWithdrawReq.builder()
                    .confirmationText(member.getNickname())
                    .build();

            // when
            memberWithdrawalCommandService.selfWithdraw(member, request);

            // then
            assertThat(memberCouponRepository.findByMemberOrderByCreatedAtDesc(member)).isEmpty();
        }

        @Test
        @DisplayName("본인이 신고자인 신고는 삭제되고, 본인이 피신고자인 신고는 보존된다")
        void selfWithdraw_deletesOwnComplaintsOnlyAsComplainant() {
            // given
            Member other = memberRepository.save(Member.builder()
                    .clerkId("clerk_other")
                    .name("상대방")
                    .nickname("otheruser")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            Complaint filedByMe = complaintRepository.save(Complaint.builder()
                    .complainant(member)
                    .respondentMember(other)
                    .targetType(ComplaintTargetType.CHAT)
                    .targetId(1L)
                    .reason("부적절한 콘텐츠입니다.")
                    .build());
            Complaint filedAgainstMe = complaintRepository.save(Complaint.builder()
                    .complainant(other)
                    .respondentMember(member)
                    .targetType(ComplaintTargetType.CHAT)
                    .targetId(2L)
                    .reason("부적절한 콘텐츠입니다.")
                    .build());

            MemberReqDTO.SelfWithdrawReq request = MemberReqDTO.SelfWithdrawReq.builder()
                    .confirmationText(member.getNickname())
                    .build();

            // when
            memberWithdrawalCommandService.selfWithdraw(member, request);

            // then
            assertThat(complaintRepository.findById(filedByMe.getId())).isEmpty();
            assertThat(complaintRepository.findById(filedAgainstMe.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("getWithdrawalPreview")
    class GetWithdrawalPreviewTest {

        @Test
        @DisplayName("잔여 생성권과 쿠폰 수를 조회한다")
        void getWithdrawalPreview_returnsCounts() {
            // given
            memberReportCreditRepository.save(MemberReportCredit.of(member, 5));

            // when
            MemberResDTO.WithdrawalPreviewDTO result = memberWithdrawalCommandService.getWithdrawalPreview(member);

            // then
            assertThat(result.remainingReportCredits()).isEqualTo(5);
            assertThat(result.couponCount()).isZero();
            assertThat(result.dataScope()).isNotEmpty();
        }
    }
}
