package com.umc.devine.global.scheduler;

import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.enums.ImageType;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberStatusHistory;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.enums.MemberStatusAction;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberAgreementRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.member.repository.TermsRepository;
import com.umc.devine.domain.member.service.command.MemberCommandService;
import com.umc.devine.domain.member.service.command.MemberWithdrawalCommandService;
import com.umc.devine.domain.notification.entity.Notification;
import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.ticket.entity.CreditRefundRequest;
import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import com.umc.devine.domain.ticket.repository.CreditRefundRequestRepository;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 스케줄러가 회원별 독립 트랜잭션(REQUIRES_NEW)으로 동작해 테스트의 기본 롤백 방식으론 검증이 안 되므로, 트랜잭션 래핑을 끄고 직접 정리한다. */
@TestPropertySource(properties = "member.hard-delete.enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberHardDeleteSchedulerTest extends IntegrationTestSupport {

    @Autowired
    private MemberHardDeleteScheduler scheduler;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MemberStatusHistoryRepository memberStatusHistoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private MemberWithdrawalCommandService memberWithdrawalCommandService;

    @Autowired
    private TermsRepository termsRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberReportCreditRepository memberReportCreditRepository;

    @Autowired
    private CreditRefundRequestRepository creditRefundRequestRepository;

    @Autowired
    private MemberAgreementRepository memberAgreementRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private TransactionTemplate transactionTemplate;

    private Member cleanWithdrawnMember;
    private Member withdrawnMemberWithPayment;
    private Member notYetDueMember;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        // 이전 실행이 중간에 실패해 정리되지 못한 잔여 데이터가 있으면 먼저 제거한다 (Postgres 컨테이너 재사용 대비).
        transactionTemplate.executeWithoutResult(status -> {
            List<String> testClerkIds = List.of(
                    "deleted-clean", "deleted-with-payment", "deleted-recent",
                    "clerk_hard_delete_real_flow", "clerk_hard_delete_real_flow_counterpart",
                    "clerk_hard_delete_refund_request");
            memberRepository.findAll().stream()
                    .filter(m -> testClerkIds.contains(m.getClerkId()))
                    .forEach(m -> {
                        paymentRepository.findAll().stream()
                                .filter(p -> p.getMember().getId().equals(m.getId()))
                                .forEach(paymentRepository::delete);
                        notificationRepository.bulkDeleteByReceiver(m);
                        notificationRepository.bulkNullifySender(m);
                        imageRepository.bulkNullifyUploader(m);
                        bookmarkRepository.bulkDeleteByMember(m);
                        memberStatusHistoryRepository.bulkDeleteByMember(m);
                        creditRefundRequestRepository.findAll().stream()
                                .filter(r -> r.getMember() != null && r.getMember().getId().equals(m.getId()))
                                .forEach(creditRefundRequestRepository::delete);
                        memberReportCreditRepository.bulkDeleteByMember(m);
                        memberAgreementRepository.bulkDeleteByMember(m);
                        contactRepository.deleteAllByMember(m);
                        memberRepository.delete(m);
                    });
        });

        cleanWithdrawnMember = memberRepository.save(Member.builder()
                .clerkId("deleted-clean")
                .nickname("deleted-clean-nick")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.DELETED)
                .build());
        forceSetDeletedAt(cleanWithdrawnMember.getId(), LocalDateTime.now().minusDays(31));
        // 실제 자진탈퇴 플로우에서는 반드시 남는 이력(NOT NULL FK)이라 재현해서 하드삭제가 안 막히는지 검증한다.
        memberStatusHistoryRepository.save(MemberStatusHistory.builder()
                .member(cleanWithdrawnMember)
                .action(MemberStatusAction.SELF_WITHDRAW)
                .status(MemberStatus.DELETED)
                .build());

        withdrawnMemberWithPayment = memberRepository.save(Member.builder()
                .clerkId("deleted-with-payment")
                .nickname("deleted-payment-nick")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.DELETED)
                .build());
        contactRepository.save(Contact.builder()
                .contactType(ContactType.EMAIL)
                .value("stillhaspayment@example.com")
                .member(withdrawnMemberWithPayment)
                .build());
        paymentRepository.save(Payment.builder()
                .portonePaymentId("payment_hard_delete_test_" + System.nanoTime())
                .member(withdrawnMemberWithPayment)
                .orderName("리포트 열람권 1개")
                .amount(4900L)
                .currency("KRW")
                .build());
        forceSetDeletedAt(withdrawnMemberWithPayment.getId(), LocalDateTime.now().minusDays(31));

        notYetDueMember = memberRepository.save(Member.builder()
                .clerkId("deleted-recent")
                .nickname("deleted-recent-nick")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.DELETED)
                .build());
        forceSetDeletedAt(notYetDueMember.getId(), LocalDateTime.now().minusDays(1));
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            paymentRepository.findAll().stream()
                    .filter(p -> p.getMember().getId().equals(withdrawnMemberWithPayment.getId()))
                    .forEach(paymentRepository::delete);
            memberRepository.findById(withdrawnMemberWithPayment.getId())
                    .ifPresent(contactRepository::deleteAllByMember);
            memberRepository.findById(cleanWithdrawnMember.getId()).ifPresent(memberRepository::delete);
            memberRepository.findById(withdrawnMemberWithPayment.getId()).ifPresent(memberRepository::delete);
            memberRepository.findById(notYetDueMember.getId()).ifPresent(memberRepository::delete);
        });
    }

    private void forceSetDeletedAt(Long memberId, LocalDateTime deletedAt) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.finalizeWithdrawal();
        setDeletedAtViaReflection(member, deletedAt);
        memberRepository.save(member);
    }

    private void setDeletedAtViaReflection(Member member, LocalDateTime deletedAt) {
        try {
            var field = Member.class.getDeclaredField("deletedAt");
            field.setAccessible(true);
            field.set(member, deletedAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("유예기간이 지나고 참조 레코드가 없는 회원은 상태변경 이력까지 포함해 하드삭제된다")
    void hardDeleteExpiredWithdrawals_deletesCleanMember() {
        // when
        scheduler.hardDeleteExpiredWithdrawals();

        // then
        assertThat(memberRepository.findById(cleanWithdrawnMember.getId())).isEmpty();
        assertThat(memberStatusHistoryRepository.findByMemberIdOrderByCreatedAtDesc(cleanWithdrawnMember.getId())).isEmpty();
    }

    @Test
    @DisplayName("결제 기록이 남아있는 회원은 FK 위반으로 건너뛰고 삭제되지 않는다")
    void hardDeleteExpiredWithdrawals_skipsMemberWithPayment() {
        // when
        scheduler.hardDeleteExpiredWithdrawals();

        // then
        assertThat(memberRepository.findById(withdrawnMemberWithPayment.getId())).isPresent();
    }

    @Test
    @DisplayName("유예기간이 지나지 않은 회원은 삭제 대상에서 제외된다")
    void hardDeleteExpiredWithdrawals_skipsNotYetDueMember() {
        // when
        scheduler.hardDeleteExpiredWithdrawals();

        // then
        assertThat(memberRepository.findById(notYetDueMember.getId())).isPresent();
    }

    @Test
    @DisplayName("실제 가입→자진탈퇴 플로우를 거친 회원도 하드삭제된다 (member_report_credit/member_agreement가 실제로 생성된 상태)")
    void hardDeleteExpiredWithdrawals_deletesRealSignupAndSelfWithdrawnMember() {
        // given: 실제 회원가입 플로우. signup 시 member_report_credit, member_agreement가 항상 생성된다
        List<Terms> requiredTerms = termsRepository.findAllByRequired(true);
        Category category = categoryRepository.findByGenre(CategoryGenre.HEALTHCARE).orElseThrow();
        ClerkPrincipal principal = new ClerkPrincipal("clerk_hard_delete_real_flow", "realflow@example.com", "실플로우", null);
        memberCommandService.signup(principal, MemberReqDTO.SignupDTO.builder()
                .agreements(requiredTerms.stream()
                        .map(t -> MemberReqDTO.AgreementDTO.builder().termsId(t.getId()).agreed(true).build())
                        .toList())
                .nickname("realflowuser")
                .mainType(MemberMainType.DEVELOPER)
                .categoryIds(List.of(category.getId()))
                .build());
        Member realFlowMember = memberRepository.findByClerkId("clerk_hard_delete_real_flow").orElseThrow();
        Long realFlowMemberId = realFlowMember.getId();
        assertThat(memberReportCreditRepository.findByMember(realFlowMember)).isPresent();
        assertThat(memberAgreementRepository.findAllByMember(realFlowMember)).isNotEmpty();

        // 순수 개인 데이터: 북마크
        Bookmark bookmark = bookmarkRepository.save(Bookmark.builder()
                .member(realFlowMember)
                .targetType(BookmarkType.PROJECT)
                .targetId(1L)
                .build());

        // 업로드 이미지: 삭제 대상 아님, 업로더 참조만 끊겨야 함
        Image image = imageRepository.save(Image.builder()
                .imageType(ImageType.PROFILE)
                .imageUrl("https://example.com/real-flow-" + System.nanoTime() + ".png")
                .s3Key("profile/real-flow-" + System.nanoTime() + ".png")
                .uploaded(true)
                .uploader(realFlowMember)
                .build());

        // 알림: 내가 받은 알림(삭제 대상) + 내가 상대방에게 보낸 알림(상대방 알림함엔 남고 발신자만 끊김)
        Member counterpart = memberRepository.save(Member.builder()
                .clerkId("clerk_hard_delete_real_flow_counterpart")
                .nickname("realflowcounterpart")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
        Notification receivedByMe = notificationRepository.save(Notification.builder()
                .receiver(realFlowMember)
                .sender(counterpart)
                .type(NotificationType.MATCHING_APPLIED)
                .title("알림")
                .content("내용")
                .build());
        Notification sentByMe = notificationRepository.save(Notification.builder()
                .receiver(counterpart)
                .sender(realFlowMember)
                .type(NotificationType.MATCHING_APPLIED)
                .title("알림")
                .content("내용")
                .build());

        // 실제 자진탈퇴 플로우: member_status_history가 생성된다
        memberWithdrawalCommandService.selfWithdraw(realFlowMember,
                MemberReqDTO.SelfWithdrawReq.builder().confirmationText("realflowuser").build());
        assertThat(memberStatusHistoryRepository.findByMemberIdOrderByCreatedAtDesc(realFlowMemberId)).isNotEmpty();

        forceSetDeletedAt(realFlowMemberId, LocalDateTime.now().minusDays(31));

        // when
        scheduler.hardDeleteExpiredWithdrawals();

        // then
        assertThat(memberRepository.findById(realFlowMemberId)).isEmpty();
        assertThat(bookmarkRepository.findById(bookmark.getId())).isEmpty();
        assertThat(imageRepository.findById(image.getId())).isPresent();
        assertThat(imageRepository.findById(image.getId()).orElseThrow().getUploader()).isNull();
        assertThat(notificationRepository.findById(receivedByMe.getId())).isEmpty();
        assertThat(notificationRepository.findById(sentByMe.getId())).isPresent();
        assertThat(notificationRepository.findById(sentByMe.getId()).orElseThrow().getSender()).isNull();

        // cleanup
        transactionTemplate.executeWithoutResult(status -> {
            notificationRepository.findById(sentByMe.getId()).ifPresent(notificationRepository::delete);
            imageRepository.findById(image.getId()).ifPresent(imageRepository::delete);
            memberRepository.findById(counterpart.getId()).ifPresent(memberRepository::delete);
        });
    }

    @Test
    @DisplayName("미처리 환불 신청이 남아있는 회원도 하드삭제되지만, 환불 신청 자체는 EXPIRED로 남고 삭제되지 않는다")
    void hardDeleteExpiredWithdrawals_expiresUnprocessedCreditRefundRequestInsteadOfDeleting() {
        // given: 실제 가입 → 잔여 생성권 환불 신청과 함께 자진탈퇴 (credit_refund_request 로우 생성)
        List<Terms> requiredTerms = termsRepository.findAllByRequired(true);
        Category category = categoryRepository.findByGenre(CategoryGenre.HEALTHCARE).orElseThrow();
        ClerkPrincipal principal = new ClerkPrincipal("clerk_hard_delete_refund_request", "refundrequest@example.com", "환불요청", null);
        memberCommandService.signup(principal, MemberReqDTO.SignupDTO.builder()
                .agreements(requiredTerms.stream()
                        .map(t -> MemberReqDTO.AgreementDTO.builder().termsId(t.getId()).agreed(true).build())
                        .toList())
                .nickname("refundrequestuser")
                .mainType(MemberMainType.DEVELOPER)
                .categoryIds(List.of(category.getId()))
                .build());
        Member member = memberRepository.findByClerkId("clerk_hard_delete_refund_request").orElseThrow();
        Long memberId = member.getId();
        assertThat(memberReportCreditRepository.findByMember(member).orElseThrow().getRemainingCount()).isGreaterThan(0);

        memberWithdrawalCommandService.selfWithdraw(member,
                MemberReqDTO.SelfWithdrawReq.builder().confirmationText("refundrequestuser").refundRequested(true).build());
        List<CreditRefundRequest> refundRequests = creditRefundRequestRepository
                .findAllByStatus(CreditRefundStatus.REQUESTED, Pageable.unpaged()).getContent().stream()
                .filter(r -> r.getMember().getId().equals(memberId))
                .toList();
        assertThat(refundRequests).isNotEmpty();

        forceSetDeletedAt(memberId, LocalDateTime.now().minusDays(31));

        // when
        scheduler.hardDeleteExpiredWithdrawals();

        // then: 회원은 하드삭제되지만, 환불 청구 기록은 삭제되지 않고 EXPIRED로 남아 감사 추적이 보존된다
        assertThat(memberRepository.findById(memberId)).isEmpty();
        List<CreditRefundRequest> survivingRequests = refundRequests.stream()
                .map(r -> creditRefundRequestRepository.findById(r.getId()).orElseThrow())
                .toList();
        assertThat(survivingRequests).hasSameSizeAs(refundRequests);
        assertThat(survivingRequests).allSatisfy(r -> {
            assertThat(r.getStatus()).isEqualTo(CreditRefundStatus.EXPIRED);
            assertThat(r.getMember()).isNull();
            assertThat(r.getProcessedAt()).isNotNull();
        });

        // cleanup
        transactionTemplate.executeWithoutResult(status ->
                survivingRequests.forEach(creditRefundRequestRepository::delete));
    }
}
