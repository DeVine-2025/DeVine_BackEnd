package com.umc.devine.admin.dashboard.service.query;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.admin.dashboard.dto.AdminDashboardResDTO;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.payment.repository.TransactionRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDashboardQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private AdminDashboardQueryService adminDashboardQueryService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        paymentRepository.deleteAll();
        complaintRepository.deleteAll();
        couponRepository.deleteAll();

        member = memberRepository.save(Member.builder()
                .clerkId("clerk_dashboard")
                .name("대시보드")
                .nickname("dashboard")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("신고 대기 건수, 쿠폰 사용률, 오늘 결제 건수를 함께 조회한다")
    void getDashboard_success() {
        // given
        createComplaint(ComplaintStatus.PENDING);
        createComplaint(ComplaintStatus.PENDING);
        createComplaint(ComplaintStatus.IN_REVIEW);
        createComplaint(ComplaintStatus.COMPLETED);

        createCoupon(100, 30);
        createCoupon(100, 5);

        createPaidTransaction(LocalDateTime.now());
        createPaidTransaction(LocalDate.now().atStartOfDay());

        // when
        AdminDashboardResDTO.DashboardDTO result = adminDashboardQueryService.getDashboard();

        // then
        assertThat(result.pendingComplaintCount()).isEqualTo(2L);
        assertThat(result.couponUsageRate()).isEqualTo(17.5);
        assertThat(result.todayPaymentCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("발급된 쿠폰이 없으면 사용률은 0.0이다")
    void getDashboard_noIssuedCoupon() {
        // given
        createCoupon(0, 0);

        // when
        AdminDashboardResDTO.DashboardDTO result = adminDashboardQueryService.getDashboard();

        // then
        assertThat(result.couponUsageRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("쿠폰이 하나도 없어도 사용률은 0.0이다")
    void getDashboard_noCoupon() {
        AdminDashboardResDTO.DashboardDTO result = adminDashboardQueryService.getDashboard();

        assertThat(result.couponUsageRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("어제 결제와 내일 결제는 오늘 결제 건수에 포함되지 않는다")
    void getDashboard_todayPaymentBoundary() {
        // given
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        createPaidTransaction(startOfToday.minusSeconds(1));
        createPaidTransaction(startOfToday.plusDays(1));
        createPaidTransaction(startOfToday.plusDays(1).minusSeconds(1));

        // when
        AdminDashboardResDTO.DashboardDTO result = adminDashboardQueryService.getDashboard();

        // then
        assertThat(result.todayPaymentCount()).isEqualTo(1L);
    }

    private void createComplaint(ComplaintStatus status) {
        complaintRepository.save(Complaint.builder()
                .complainant(member)
                .respondentMember(member)
                .targetType(ComplaintTargetType.CHAT)
                .targetId(1L)
                .reason("부적절한 콘텐츠입니다.")
                .status(status)
                .build());
    }

    private void createCoupon(int issuedCount, int usedCount) {
        couponRepository.save(Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(1000L)
                .issuedCount(issuedCount)
                .usedCount(usedCount)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .build());
    }

    private void createPaidTransaction(LocalDateTime paidAt) {
        Payment payment = paymentRepository.save(Payment.builder()
                .portonePaymentId("payment_" + paidAt.toString() + "_" + System.nanoTime())
                .member(member)
                .orderName("테스트 상품")
                .amount(10000L)
                .currency("KRW")
                .build());

        transactionRepository.save(Transaction.builder()
                .portoneTransactionId("tx_" + paidAt + "_" + System.nanoTime())
                .payment(payment)
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PAID)
                .method(PaymentMethod.CARD)
                .pgProvider("TEST")
                .amount(10000L)
                .paidAt(paidAt)
                .build());
    }
}
