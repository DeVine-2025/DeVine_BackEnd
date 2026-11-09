package com.umc.devine.domain.member;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.service.command.MemberWithdrawalCommandService;
import com.umc.devine.domain.member.service.query.MemberWithdrawalQueryService;
import com.umc.devine.domain.notification.entity.Notification;
import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.domain.ticket.repository.TicketProductRepository;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemberWithdrawalPreviewCancelTest extends IntegrationTestSupport {

    @Autowired private MemberWithdrawalQueryService queryService;
    @Autowired private MemberWithdrawalCommandService commandService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectRequirementMemberRepository requirementMemberRepository;
    @Autowired private MatchingRepository matchingRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private GitRepoUrlRepository gitRepoUrlRepository;
    @Autowired private DevReportRepository devReportRepository;
    @Autowired private MemberReportCreditRepository memberReportCreditRepository;
    @Autowired private TicketProductRepository ticketProductRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentRefundRepository paymentRefundRepository;
    @Autowired private EntityManager em;

    private Member pm;
    private Member dev;
    private Member dev2;
    private Category category;

    @BeforeEach
    void setUp() {
        pm = saveMember("clerk_wd_pm", "wdpm", MemberMainType.PM, MemberStatus.ACTIVE);
        dev = saveMember("clerk_wd_dev", "wddev", MemberMainType.DEVELOPER, MemberStatus.ACTIVE);
        dev2 = saveMember("clerk_wd_dev2", "wddev2", MemberMainType.DEVELOPER, MemberStatus.ACTIVE);
        category = categoryRepository.findByGenre(CategoryGenre.ECOMMERCE).orElseThrow();
    }

    private Member saveMember(String clerkId, String nickname, MemberMainType type, MemberStatus status) {
        return memberRepository.save(Member.builder()
                .clerkId(clerkId).name(nickname).nickname(nickname)
                .mainType(type).disclosure(true).used(status)
                .build());
    }

    private Project saveProject(String name, ProjectStatus status) {
        Project project = projectRepository.save(Project.builder()
                .name(name).content("소개").status(status)
                .projectField(ProjectField.WEB).mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(10)).category(category).member(pm)
                .build());
        project.addRequirement(requirementMemberRepository.save(ProjectRequirementMember.builder()
                .project(project).part(ProjectPart.BACKEND).requirementNum(2).currentCount(0)
                .build()));
        return project;
    }

    private Matching saveMatching(Project project, Member developer, MatchingType type, MatchingStatus status) {
        return matchingRepository.save(Matching.builder()
                .project(project).member(developer).matchingType(type).status(status).part(ProjectPart.BACKEND)
                .build());
    }

    private List<Notification> notificationsTo(Member receiver) {
        em.flush();
        return notificationRepository.findAll().stream()
                .filter(n -> n.getReceiver().getId().equals(receiver.getId()))
                .toList();
    }

    private ClerkPrincipal principal(Member member, String email) {
        return ClerkPrincipal.builder().clerkId(member.getClerkId()).email(email).build();
    }

    @Test
    @DisplayName("미리보기: 보낸 지원/받은 제안(개발자 쪽)과 받은 지원/보낸 제안(PM 쪽)을 대기 건으로 보여 주고, 처리된 건은 뺀다")
    void preview_pendingDirections() {
        Project applied = saveProject("지원한 프로젝트", ProjectStatus.RECRUITING);
        Project proposed = saveProject("제안받은 프로젝트", ProjectStatus.RECRUITING);
        Project done = saveProject("처리된 프로젝트", ProjectStatus.RECRUITING);
        saveMatching(applied, dev, MatchingType.APPLY, MatchingStatus.PENDING);
        saveMatching(proposed, dev, MatchingType.PROPOSE, MatchingStatus.PENDING);
        saveMatching(done, dev, MatchingType.APPLY, MatchingStatus.COMPLETED);
        saveMatching(done, dev2, MatchingType.APPLY, MatchingStatus.CANCELLED);

        WithdrawalResDTO.PreviewDTO devView = queryService.getPreview(dev, principal(dev, null));

        assertThat(devView.pendingCount()).isEqualTo(2);
        assertThat(devView.pendingMatchings())
                .extracting(WithdrawalResDTO.PendingMatchingDTO::projectName,
                        WithdrawalResDTO.PendingMatchingDTO::counterpartNickname,
                        WithdrawalResDTO.PendingMatchingDTO::matchingType,
                        WithdrawalResDTO.PendingMatchingDTO::direction)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("지원한 프로젝트", "wdpm", MatchingType.APPLY, WithdrawalResDTO.Direction.SENT),
                        org.assertj.core.groups.Tuple.tuple("제안받은 프로젝트", "wdpm", MatchingType.PROPOSE, WithdrawalResDTO.Direction.RECEIVED));

        WithdrawalResDTO.PreviewDTO pmView = queryService.getPreview(pm, principal(pm, null));

        assertThat(pmView.pendingMatchings())
                .extracting(WithdrawalResDTO.PendingMatchingDTO::counterpartNickname,
                        WithdrawalResDTO.PendingMatchingDTO::direction)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("wddev", WithdrawalResDTO.Direction.RECEIVED),
                        org.assertj.core.groups.Tuple.tuple("wddev", WithdrawalResDTO.Direction.SENT));
    }

    @Test
    @DisplayName("미리보기: 리포트 수는 완료된 MAIN만 세고, 생성 중인 리포트가 있으면 알려 준다")
    void preview_reportCount() {
        GitRepoUrl done = gitRepoUrlRepository.save(GitRepoUrl.builder().gitUrl("https://github.com/wd/done").member(dev).build());
        GitRepoUrl generating = gitRepoUrlRepository.save(GitRepoUrl.builder().gitUrl("https://github.com/wd/gen").member(dev).build());
        GitRepoUrl failed = gitRepoUrlRepository.save(GitRepoUrl.builder().gitUrl("https://github.com/wd/fail").member(dev).build());
        LocalDateTime now = LocalDateTime.now();
        devReportRepository.save(DevReport.builder().gitRepoUrl(done).reportType(ReportType.MAIN).completedAt(now).build());
        devReportRepository.save(DevReport.builder().gitRepoUrl(done).reportType(ReportType.DETAIL).completedAt(now).build());
        devReportRepository.save(DevReport.builder().gitRepoUrl(generating).reportType(ReportType.MAIN).build());
        devReportRepository.save(DevReport.builder().gitRepoUrl(failed).reportType(ReportType.MAIN).errorMessage("실패").build());

        WithdrawalResDTO.PreviewDTO preview = queryService.getPreview(dev, principal(dev, null));

        assertThat(preview.reportCount()).isEqualTo(1);
        assertThat(preview.generatingReport()).isTrue();
    }

    @Test
    @DisplayName("미리보기 T4: 3개 묶음(9,900원) 사서 1개 사용, 20일 뒤 → 환불 가능 2개, 6,600원")
    void preview_refund_t4() {
        memberReportCreditRepository.save(MemberReportCredit.of(dev, 2));
        savePayment(dev, "pay_wd_t4", 9_900L, bundle(), 1, LocalDateTime.now().minusDays(20));

        WithdrawalResDTO.PreviewDTO preview = queryService.getPreview(dev, principal(dev, "dev@example.com"));

        assertThat(preview.remainingCredits()).isEqualTo(2);
        assertThat(preview.refundableCredits()).isEqualTo(2);
        assertThat(preview.nonRefundableCredits()).isZero();
        assertThat(preview.refundAmount()).isEqualTo(6_600L);
        assertThat(preview.refundContactEmail()).isEqualTo("dev@example.com");
    }

    @Test
    @DisplayName("미리보기: 관리자가 결제 전체를 환불한 건과 0원 결제는 환불 대상이 아니다")
    void preview_refund_excludesAdminRefundedAndFreePayments() {
        memberReportCreditRepository.save(MemberReportCredit.of(dev, 4));
        Payment refunded = savePayment(dev, "pay_wd_admin", 9_900L, bundle(), 1, LocalDateTime.now().minusDays(5));
        PaymentRefund adminRefund = PaymentRefund.claim(refunded, "고객 요청");
        adminRefund.complete("cancel_wd_admin");
        paymentRefundRepository.save(adminRefund);
        savePayment(dev, "FREE_WD", 0L, bundle(), 1, LocalDateTime.now().minusDays(1));

        WithdrawalResDTO.PreviewDTO preview = queryService.getPreview(dev, principal(dev, null));

        assertThat(preview.refundableCredits()).isZero();
        assertThat(preview.nonRefundableCredits()).isEqualTo(4);
        assertThat(preview.refundAmount()).isZero();
    }

    @Test
    @DisplayName("T3: 대기 지원 2건 있는 PM이 일괄 취소하면 2건이 취소되고, 지원자 2명에게 탈퇴 사실 없이 알림이 간다")
    void t3_pmCancelsPendingApplications() {
        Project project = saveProject("모집 프로젝트", ProjectStatus.RECRUITING);
        Matching first = saveMatching(project, dev, MatchingType.APPLY, MatchingStatus.PENDING);
        Matching second = saveMatching(project, dev2, MatchingType.APPLY, MatchingStatus.PENDING);

        WithdrawalResDTO.CancelPendingResultDTO result = commandService.cancelPendingMatchings(pm);

        assertThat(result.canceledCount()).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(MatchingStatus.CANCELLED);
        assertThat(second.getStatus()).isEqualTo(MatchingStatus.CANCELLED);
        for (Member applicant : List.of(dev, dev2)) {
            assertThat(notificationsTo(applicant)).singleElement().satisfies(n -> {
                assertThat(n.getType()).isEqualTo(NotificationType.MATCHING_APPLICATION_CANCELLED);
                assertThat(n.getSender().getId()).isEqualTo(pm.getId());
                assertThat(n.getContent()).isEqualTo("'모집 프로젝트' 프로젝트 지원이 취소됐어요.");
                assertThat(n.getContent()).doesNotContain("탈퇴");
            });
        }
        assertThat(queryService.getPreview(pm, principal(pm, null)).pendingCount()).isZero();
    }

    @Test
    @DisplayName("개발자가 일괄 취소하면 보낸 지원과 받은 제안이 취소되고 PM에게 각각 지원/제안 취소 알림이 간다")
    void developerCancelsSentApplicationAndReceivedProposal() {
        Project applied = saveProject("지원한 프로젝트", ProjectStatus.RECRUITING);
        Project proposed = saveProject("제안받은 프로젝트", ProjectStatus.RECRUITING);
        saveMatching(applied, dev, MatchingType.APPLY, MatchingStatus.PENDING);
        saveMatching(proposed, dev, MatchingType.PROPOSE, MatchingStatus.PENDING);

        assertThat(commandService.cancelPendingMatchings(dev).canceledCount()).isEqualTo(2);

        assertThat(notificationsTo(pm))
                .extracting(Notification::getType, Notification::getContent)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(NotificationType.MATCHING_APPLICATION_CANCELLED, "'지원한 프로젝트' 프로젝트 지원이 취소됐어요."),
                        org.assertj.core.groups.Tuple.tuple(NotificationType.MATCHING_PROPOSAL_CANCELLED, "'제안받은 프로젝트' 프로젝트 제안이 취소됐어요."));
    }

    @Test
    @DisplayName("P18: 마감일 지남 상태에서 대기 건이 탈퇴자의 지원 1건뿐이면, 일괄 취소 즉시 마감된다")
    void p18_deadlinePassedProjectClosesAfterCancel() {
        Project project = saveProject("마감일 지난 프로젝트", ProjectStatus.DEADLINE_PASSED);
        saveMatching(project, dev, MatchingType.APPLY, MatchingStatus.PENDING);

        commandService.cancelPendingMatchings(dev);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CLOSED);
        assertThat(project.getClosedBy()).isNull();
        assertThat(project.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("다른 사람의 대기 건이 남아 있으면 마감일 지남 상태를 유지한다")
    void deadlinePassedProjectStaysWhenOthersPending() {
        Project project = saveProject("마감일 지난 프로젝트", ProjectStatus.DEADLINE_PASSED);
        saveMatching(project, dev, MatchingType.APPLY, MatchingStatus.PENDING);
        Matching other = saveMatching(project, dev2, MatchingType.APPLY, MatchingStatus.PENDING);

        commandService.cancelPendingMatchings(dev);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DEADLINE_PASSED);
        assertThat(other.getStatus()).isEqualTo(MatchingStatus.PENDING);
    }

    @Test
    @DisplayName("대기 건이 없으면 0건으로 성공한다")
    void noPending_returnsZero() {
        assertThat(commandService.cancelPendingMatchings(dev).canceledCount()).isZero();
        assertThat(notificationsTo(pm)).isEmpty();
    }

    @Test
    @DisplayName("탈퇴 API용 회원 조회는 정지/강제탈퇴 예정 회원을 찾고, 탈퇴 회원은 찾지 않는다")
    void sanctionedMembersResolvableForWithdrawal() {
        Member suspended = saveMember("clerk_wd_suspended", "wdsusp", MemberMainType.DEVELOPER, MemberStatus.SUSPENDED);
        Member pending = saveMember("clerk_wd_pending", "wdpend", MemberMainType.DEVELOPER, MemberStatus.PENDING_WITHDRAWAL);
        Member deleted = saveMember("clerk_wd_deleted", "wddel", MemberMainType.DEVELOPER, MemberStatus.DELETED);
        List<MemberStatus> allowed = List.of(MemberStatus.ACTIVE, MemberStatus.SUSPENDED, MemberStatus.PENDING_WITHDRAWAL);

        assertThat(memberRepository.findByClerkIdAndUsedIn(suspended.getClerkId(), allowed)).isPresent();
        assertThat(memberRepository.findByClerkIdAndUsedIn(pending.getClerkId(), allowed)).isPresent();
        assertThat(memberRepository.findByClerkIdAndUsedIn(deleted.getClerkId(), allowed)).isEmpty();
        assertThat(memberRepository.findByClerkId(suspended.getClerkId())).isEmpty();
    }

    private TicketProduct bundle() {
        return ticketProductRepository.save(TicketProduct.builder()
                .name("리포트 생성권 3개").price(9_900L).creditAmount(3).active(true)
                .build());
    }

    private Payment savePayment(Member member, String portonePaymentId, long amount, TicketProduct product,
                                int quantity, LocalDateTime paidAt) {
        Payment payment = Payment.builder()
                .portonePaymentId(portonePaymentId).member(member).orderName(product.getName())
                .amount(amount).currency("KRW")
                .build();
        payment.addTransaction(Transaction.builder()
                .portoneTransactionId("tx_" + portonePaymentId).payment(payment)
                .type(TransactionType.PAYMENT).status(TransactionStatus.PAID)
                .method(PaymentMethod.CARD).pgProvider("TOSSPAYMENTS")
                .amount(amount).paidAt(paidAt)
                .build());
        payment.addPaymentTicket(PaymentTicket.builder()
                .payment(payment).ticketProduct(product).quantity(quantity)
                .unitPrice(product.getPrice()).unitCreditAmount(product.getCreditAmount())
                .build());
        return paymentRepository.saveAndFlush(payment);
    }
}
