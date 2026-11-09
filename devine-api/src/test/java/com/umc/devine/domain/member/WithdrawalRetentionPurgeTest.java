package com.umc.devine.domain.member;

import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintReasonCode;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.complaint.repository.ComplaintRepository;
import com.umc.devine.admin.member.entity.MemberStatusHistory;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.admin.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberRestriction;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.MemberRestrictionRepository;
import com.umc.devine.domain.member.service.command.WithdrawalRetentionPurgeService;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.entity.MemberCoupon;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WithdrawalRetentionPurgeTest extends IntegrationTestSupport {

    @Autowired private WithdrawalRetentionPurgeService purgeService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberStatusHistoryRepository memberStatusHistoryRepository;
    @Autowired private MemberRestrictionRepository memberRestrictionRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private MatchingRepository matchingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentRefundRepository paymentRefundRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private MemberCouponRepository memberCouponRepository;
    @Autowired private EntityManager em;

    private final LocalDateTime now = LocalDateTime.now();
    private Category category;
    private int seq;

    @BeforeEach
    void setUp() {
        category = categoryRepository.findByGenre(CategoryGenre.ECOMMERCE).orElseThrow();
    }

    private Member activeMember() {
        return memberRepository.save(Member.builder().clerkId("clerk_rp_" + seq).nickname("rp" + seq++)
                .mainType(MemberMainType.DEVELOPER).used(MemberStatus.ACTIVE).build());
    }

    private Member withdrawnMember(LocalDateTime withdrawnAt) {
        Member member = activeMember();
        member.anonymizeForWithdrawal();
        MemberStatusHistory history = memberStatusHistoryRepository.save(MemberStatusHistory.builder()
                .member(member).action(MemberStatusAction.SELF_WITHDRAW).status(MemberStatus.DELETED).build());
        em.flush();
        setCreatedAt("member_status_history", "member_status_history_id", history.getId(), withdrawnAt);
        return member;
    }

    private void setCreatedAt(String table, String idColumn, Long id, LocalDateTime createdAt) {
        em.createNativeQuery("UPDATE " + table + " SET created_at = :at WHERE " + idColumn + " = :id")
                .setParameter("at", createdAt).setParameter("id", id).executeUpdate();
    }

    private Project project(Member pm, ProjectStatus status, boolean hidden) {
        Project project = projectRepository.save(Project.builder()
                .name("프로젝트" + seq++).content("소개").status(status)
                .projectField(ProjectField.WEB).mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(10)).category(category).member(pm)
                .build());
        if (hidden) {
            project.changeVisibilityByAdmin(false, pm, now);
        }
        return project;
    }

    private Matching matching(Project project, Member developer, MatchingType type, String content) {
        return matchingRepository.save(Matching.builder().project(project).member(developer)
                .matchingType(type).status(MatchingStatus.COMPLETED).part(ProjectPart.BACKEND).content(content).build());
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("매칭: 지원자(개발자)가 탈퇴 1년이 지나면 그 지원 내용만 비우고 행은 PM 이력으로 남긴다")
    void developerWithdrawnOverOneYear_clearsApplyContent() {
        Member pm = activeMember();
        Member dev = withdrawnMember(now.minusYears(1).minusDays(1));
        Matching apply = matching(project(pm, ProjectStatus.CLOSED, false), dev, MatchingType.APPLY, "지원 메시지");
        Matching propose = matching(project(pm, ProjectStatus.CLOSED, false), dev, MatchingType.PROPOSE, "PM 제안 메시지");
        flushAndClear();

        purgeService.purgeMatchingHistory(now);
        flushAndClear();

        assertThat(matchingRepository.findById(apply.getId()).orElseThrow().getContent()).isNull();
        assertThat(matchingRepository.findById(propose.getId()).orElseThrow().getContent()).isEqualTo("PM 제안 메시지");
    }

    @Test
    @DisplayName("매칭: 탈퇴 1년이 안 지났으면 그대로 둔다")
    void withinOneYear_untouched() {
        Member pm = activeMember();
        Member dev = withdrawnMember(now.minusMonths(11));
        Matching apply = matching(project(pm, ProjectStatus.CLOSED, false), dev, MatchingType.APPLY, "지원 메시지");
        flushAndClear();

        purgeService.purgeMatchingHistory(now);
        flushAndClear();

        assertThat(matchingRepository.findById(apply.getId()).orElseThrow().getContent()).isEqualTo("지원 메시지");
    }

    @Test
    @DisplayName("매칭: 양쪽 모두 탈퇴했으면 나중에 탈퇴한 쪽 기준 1년이 지나야 행을 지운다")
    void bothWithdrawn_deletedAfterLaterPlusOneYear() {
        Member pm = withdrawnMember(now.minusYears(3));
        Member devOld = withdrawnMember(now.minusYears(2));
        Member devRecent = withdrawnMember(now.minusMonths(6));
        Matching expired = matching(project(pm, ProjectStatus.DELETED, false), devOld, MatchingType.APPLY, null);
        Matching kept = matching(project(pm, ProjectStatus.DELETED, false), devRecent, MatchingType.PROPOSE, "제안");
        flushAndClear();

        purgeService.purgeMatchingHistory(now);
        flushAndClear();

        assertThat(matchingRepository.findById(expired.getId())).isEmpty();
        assertThat(matchingRepository.findById(kept.getId())).isPresent()
                .get().extracting(Matching::getContent).isNull();
    }

    @Test
    @DisplayName("이름만 남은 삭제 프로젝트는 매칭이 0건이면 지우고, 숨긴 활성 프로젝트와 매칭이 남은 삭제 프로젝트는 남긴다")
    void orphanDeletedProjects() {
        Member pm = activeMember();
        Project orphan = project(pm, ProjectStatus.DELETED, true);
        Project hiddenActive = project(pm, ProjectStatus.RECRUITING, true);
        Project deletedWithHistory = project(pm, ProjectStatus.DELETED, true);
        matching(deletedWithHistory, activeMember(), MatchingType.APPLY, null);
        flushAndClear();

        purgeService.purgeOrphanDeletedProjects();
        flushAndClear();

        assertThat(projectRepository.findById(orphan.getId())).isEmpty();
        assertThat(projectRepository.findById(hiddenActive.getId())).isPresent();
        assertThat(projectRepository.findById(deletedWithHistory.getId())).isPresent();
    }

    @Test
    @DisplayName("예전 방식으로 상태만 DELETED가 된 프로젝트는 임베딩이 남아 있어도 하위 데이터와 함께 지운다")
    void legacyDeletedProjectWithEmbedding_isPurged() {
        Member pm = activeMember();
        Project legacy = project(pm, ProjectStatus.DELETED, false);
        em.createNativeQuery("INSERT INTO project_embedding (project_id, status, retry_count, created_at, updated_at) "
                        + "VALUES (:id, 'PENDING', 0, now(), now())")
                .setParameter("id", legacy.getId()).executeUpdate();
        flushAndClear();

        assertThat(purgeService.purgeOrphanDeletedProjects()).isEqualTo(1);
        flushAndClear();

        assertThat(projectRepository.findById(legacy.getId())).isEmpty();
    }

    @Test
    @DisplayName("결제 5년: 쿠폰이 연결된 결제도 쿠폰의 결제 연결만 끊고 지운다")
    void expiredPaymentWithCoupon_detachesCoupon() {
        Member withdrawn = withdrawnMember(now.minusYears(5));
        Payment old = payment(withdrawn, now.minusYears(6));
        Coupon coupon = couponRepository.save(Coupon.builder()
                .name("퍼지 테스트 쿠폰").discountType(DiscountType.FIXED_AMOUNT).discountValue(1000L)
                .issuedCount(1).usedCount(1)
                .validFrom(now.minusYears(7)).validUntil(now.minusYears(6)).isActive(false).build());
        MemberCoupon memberCoupon = MemberCoupon.issueTo(withdrawn, coupon);
        memberCoupon.use(old);
        memberCouponRepository.save(memberCoupon);
        em.flush();
        setCreatedAt("payment", "payment_id", old.getId(), now.minusYears(6));
        flushAndClear();

        assertThat(purgeService.purgeExpiredPayments(now)).isEqualTo(1);
        flushAndClear();

        assertThat(paymentRepository.findById(old.getId())).isEmpty();
        assertThat(memberCouponRepository.findById(memberCoupon.getId())).isPresent();
    }

    @Test
    @DisplayName("만료된 최소식별정보를 지우고, 남겨 둔 제재 이력(자진 탈퇴 행 제외)과 신고 식별 칸을 정리한다")
    void expiredRestrictionPurged() {
        Member reporter = activeMember();
        Member sanctioned = withdrawnMember(now.minusYears(1).minusDays(2));
        memberStatusHistoryRepository.save(MemberStatusHistory.builder().member(sanctioned)
                .action(MemberStatusAction.SUSPEND).status(MemberStatus.SUSPENDED).reason("정지").build());
        Complaint complaint = complaintRepository.save(Complaint.builder().complainant(reporter).respondentMember(sanctioned)
                .targetType(ComplaintTargetType.DEVELOPER).targetId(sanctioned.getId()).reasonCode(ComplaintReasonCode.ABUSE).build());
        MemberRestriction expired = memberRestrictionRepository.save(MemberRestriction.builder()
                .member(sanctioned).emailHash("a".repeat(64)).expiresAt(now.minusDays(1)).build());
        MemberRestriction valid = memberRestrictionRepository.save(MemberRestriction.builder()
                .member(withdrawnMember(now.minusDays(1))).emailHash("b".repeat(64)).expiresAt(now.plusDays(30)).build());
        flushAndClear();

        assertThat(purgeService.purgeExpiredRestrictions(now)).isEqualTo(1);
        flushAndClear();

        assertThat(memberRestrictionRepository.findById(expired.getId())).isEmpty();
        assertThat(memberRestrictionRepository.findById(valid.getId())).isPresent();
        assertThat(memberStatusHistoryRepository.findByMemberIdOrderByCreatedAtDesc(sanctioned.getId()))
                .extracting(MemberStatusHistory::getAction).containsExactly(MemberStatusAction.SELF_WITHDRAW);
        assertThat(complaintRepository.findById(complaint.getId()).orElseThrow().getRespondentMember()).isNull();
    }

    @Test
    @DisplayName("결제 5년: 탈퇴 회원의 결제일/환불 완료일이 모두 5년 지난 건만 지우고, 활동 회원 결제와 최근 환불 건은 남긴다")
    void expiredPaymentsPurged() {
        Member withdrawn = withdrawnMember(now.minusYears(5));
        Payment old = payment(withdrawn, now.minusYears(6));
        Payment oldWithRecentRefund = payment(withdrawn, now.minusYears(6));
        PaymentRefund recentRefund = PaymentRefund.requestForWithdrawal(oldWithRecentRefund, 1_000L, 1, null);
        recentRefund.completeWithdrawalRequest();
        paymentRefundRepository.save(recentRefund);
        Payment activeOld = payment(activeMember(), now.minusYears(6));
        em.flush();
        setCreatedAt("payment", "payment_id", old.getId(), now.minusYears(6));
        setCreatedAt("payment", "payment_id", oldWithRecentRefund.getId(), now.minusYears(6));
        setCreatedAt("payment", "payment_id", activeOld.getId(), now.minusYears(6));
        em.createNativeQuery("UPDATE payment_refund SET created_at = :c, updated_at = :u WHERE refund_id = :id")
                .setParameter("c", now.minusYears(6)).setParameter("u", now.minusYears(4))
                .setParameter("id", recentRefund.getId()).executeUpdate();
        flushAndClear();

        assertThat(purgeService.purgeExpiredPayments(now)).isEqualTo(1);
        flushAndClear();

        assertThat(paymentRepository.findById(old.getId())).isEmpty();
        assertThat(paymentRepository.findById(oldWithRecentRefund.getId())).isPresent();
        assertThat(paymentRepository.findById(activeOld.getId())).isPresent();
    }

    private Payment payment(Member member, LocalDateTime paidAt) {
        Payment payment = Payment.builder()
                .portonePaymentId("pay_rp_" + seq++).member(member).orderName("리포트 생성권")
                .amount(4_900L).currency("KRW").build();
        payment.addTransaction(Transaction.builder()
                .portoneTransactionId("tx_rp_" + seq++).payment(payment)
                .type(TransactionType.PAYMENT).status(TransactionStatus.PAID)
                .method(PaymentMethod.CARD).pgProvider("TOSSPAYMENTS")
                .amount(4_900L).paidAt(paidAt).build());
        return paymentRepository.save(payment);
    }
}
