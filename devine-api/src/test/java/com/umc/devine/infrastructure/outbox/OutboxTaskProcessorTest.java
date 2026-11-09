package com.umc.devine.infrastructure.outbox;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskStatus;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.domain.outbox.repository.OutboxTaskRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.email.EmailSendException;
import com.umc.devine.infrastructure.email.EmailSender;
import com.umc.devine.infrastructure.github.GitHubApiClient;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import com.umc.devine.infrastructure.outbox.payload.WithdrawalRefundMailPayload;
import com.umc.devine.infrastructure.s3.S3Service;
import com.umc.devine.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {
        "github.oauth.client-id=test-client-id",
        "github.oauth.client-secret=test-client-secret",
        "withdrawal.operator-email=ops@example.com",
        "outbox.max-attempts=2"
})
class OutboxTaskProcessorTest extends IntegrationTestSupport {

    @Autowired private OutboxTaskProcessor processor;
    @Autowired private OutboxTaskWriter writer;
    @Autowired private OutboxTaskRepository outboxTaskRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentRefundRepository paymentRefundRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private EntityManager em;

    @MockitoBean private ClerkApiClient clerkApiClient;
    @MockitoBean private GitHubApiClient gitHubApiClient;
    @MockitoBean private S3Service s3Service;
    @MockitoBean private EmailSender emailSender;
    @MockitoBean private OutboxTaskEventListener outboxTaskEventListener;

    private final List<Long> paymentIds = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        tx().executeWithoutResult(status -> {
            outboxTaskRepository.deleteAll();
            paymentIds.forEach(id -> {
                paymentRefundRepository.findAllByPaymentIdIn(List.of(id)).forEach(paymentRefundRepository::delete);
                paymentRepository.deleteById(id);
            });
            memberIds.forEach(memberRepository::deleteById);
        });
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    private void enqueue(OutboxTaskType type, Object payload) {
        tx().executeWithoutResult(status -> writer.enqueue(type, payload));
    }

    private List<OutboxTask> tasks() {
        return outboxTaskRepository.findAll();
    }

    private void makeAllDue() {
        tx().executeWithoutResult(status -> em.createNativeQuery("UPDATE outbox_task SET next_attempt_at = now() - interval '1 minute'")
                .executeUpdate());
    }

    @Test
    @DisplayName("GitHub 권한 회수 → Clerk 삭제 → S3 삭제를 순서대로 실행하고, 성공한 작업 행은 지운다")
    void runsCleanupChainInOrder() {
        given(clerkApiClient.getGitHubAccessToken("clerk_ob")).willReturn("gho_token");
        enqueue(OutboxTaskType.GITHUB_GRANT_REVOKE, new MemberExternalCleanupPayload("clerk_ob", List.of("a.png", "b.png")));

        processor.processDue();
        verify(gitHubApiClient).revokeGrant("test-client-id", "test-client-secret", "gho_token");
        verify(clerkApiClient, never()).deleteUser(anyString());
        assertThat(tasks()).singleElement().extracting(OutboxTask::getTaskType).isEqualTo(OutboxTaskType.CLERK_USER_DELETE);

        processor.processDue();
        verify(clerkApiClient).deleteUser("clerk_ob");
        assertThat(tasks()).singleElement().satisfies(t -> {
            assertThat(t.getTaskType()).isEqualTo(OutboxTaskType.S3_OBJECT_DELETE);
            assertThat(t.getPayload()).doesNotContain("clerk_ob");
        });

        processor.processDue();
        verify(s3Service).deleteObject("a.png");
        verify(s3Service).deleteObject("b.png");
        assertThat(tasks()).isEmpty();
    }

    @Test
    @DisplayName("T9: Clerk API 장애면 작업을 남겨 1분 뒤 재시도하고, 재시도 시각 전에는 다시 실행하지 않는다")
    void t9_clerkFailureRetriedLater() {
        willThrow(new IllegalStateException("Clerk 사용자 삭제 실패: HTTP 503")).given(clerkApiClient).deleteUser("clerk_ob");
        enqueue(OutboxTaskType.CLERK_USER_DELETE, new MemberExternalCleanupPayload("clerk_ob", List.of()));

        processor.processDue();

        assertThat(tasks()).singleElement().satisfies(t -> {
            assertThat(t.getStatus()).isEqualTo(OutboxTaskStatus.PENDING);
            assertThat(t.getAttemptCount()).isEqualTo(1);
            assertThat(t.getLastError()).contains("HTTP 503");
            assertThat(t.getNextAttemptAt()).isAfter(LocalDateTime.now().plusSeconds(30));
        });
        assertThat(processor.processDue()).isZero();
    }

    @Test
    @DisplayName("재시도 한도를 넘기면 FAILED로 남기고 운영자에게 알리며, GitHub 회수가 최종 실패해도 Clerk 삭제는 이어서 기록한다")
    void finalFailure_alertsAndContinues() {
        given(clerkApiClient.getGitHubAccessToken("clerk_ob")).willReturn("gho_token");
        willThrow(new IllegalStateException("GitHub 권한 회수 실패: HTTP 500"))
                .given(gitHubApiClient).revokeGrant(anyString(), anyString(), anyString());
        enqueue(OutboxTaskType.GITHUB_GRANT_REVOKE, new MemberExternalCleanupPayload("clerk_ob", List.of()));

        processor.processDue();
        makeAllDue();
        processor.processDue();

        assertThat(tasks()).filteredOn(t -> t.getTaskType() == OutboxTaskType.GITHUB_GRANT_REVOKE)
                .singleElement().satisfies(t -> {
                    assertThat(t.getStatus()).isEqualTo(OutboxTaskStatus.FAILED);
                    assertThat(t.getAttemptCount()).isEqualTo(2);
                });
        assertThat(tasks()).filteredOn(t -> t.getTaskType() == OutboxTaskType.CLERK_USER_DELETE).hasSize(1);
        verify(emailSender).send(eq("ops@example.com"), contains("아웃박스 최종 실패"), contains("작업 ID"));
    }

    @Test
    @DisplayName("운영자 환불 메일: 제목에 건수와 총액, 본문에 결제/환불 정보와 연락 이메일을 넣고 닉네임은 넣지 않는다")
    void refundMailSent() {
        Long refundId = saveWithdrawalRefund("pay_ob_mail", 6_600L, 2, "refund@example.com");
        enqueue(OutboxTaskType.WITHDRAWAL_REFUND_MAIL, new WithdrawalRefundMailPayload(List.of(refundId)));

        processor.processDue();

        verify(emailSender).sendOrThrow(eq("ops@example.com"),
                eq("[탈퇴 환불 신청] 결제 1건 / 총 6,600원"),
                org.mockito.ArgumentMatchers.argThat(body -> body.contains("pay_ob_mail")
                        && body.contains("6,600원")
                        && body.contains("2개")
                        && body.contains("refund@example.com")
                        && !body.contains("obmember")));
        assertThat(tasks()).isEmpty();
    }

    @Test
    @DisplayName("운영자 메일 발송이 실패하면 재시도 큐에 남는다")
    void refundMailFailureRetried() {
        Long refundId = saveWithdrawalRefund("pay_ob_fail", 4_900L, 1, "refund@example.com");
        willThrow(new EmailSendException("Brevo 이메일 발송 실패", new RuntimeException("503")))
                .given(emailSender).sendOrThrow(anyString(), anyString(), anyString());
        enqueue(OutboxTaskType.WITHDRAWAL_REFUND_MAIL, new WithdrawalRefundMailPayload(List.of(refundId)));

        processor.processDue();

        assertThat(tasks()).singleElement().satisfies(t -> {
            assertThat(t.getStatus()).isEqualTo(OutboxTaskStatus.PENDING);
            assertThat(t.getAttemptCount()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("재시도 간격은 1분부터 2배씩 늘고 6시간을 넘지 않는다")
    void backoff() {
        assertThat(OutboxTaskProcessor.backoffMinutes(1)).isEqualTo(1);
        assertThat(OutboxTaskProcessor.backoffMinutes(2)).isEqualTo(2);
        assertThat(OutboxTaskProcessor.backoffMinutes(5)).isEqualTo(16);
        assertThat(OutboxTaskProcessor.backoffMinutes(20)).isEqualTo(360);
    }

    private Long saveWithdrawalRefund(String portonePaymentId, long amount, int credits, String email) {
        return tx().execute(status -> {
            Member member = memberRepository.save(Member.builder()
                    .clerkId("clerk_" + portonePaymentId).nickname("obmember" + memberIds.size())
                    .mainType(MemberMainType.DEVELOPER).used(MemberStatus.DELETED)
                    .build());
            memberIds.add(member.getId());
            Payment payment = Payment.builder()
                    .portonePaymentId(portonePaymentId).member(member).orderName("리포트 생성권")
                    .amount(9_900L).currency("KRW")
                    .build();
            payment.addTransaction(Transaction.builder()
                    .portoneTransactionId("tx_" + portonePaymentId).payment(payment)
                    .type(TransactionType.PAYMENT).status(TransactionStatus.PAID)
                    .method(PaymentMethod.CARD).pgProvider("TOSSPAYMENTS")
                    .amount(9_900L).paidAt(LocalDateTime.now().minusDays(3))
                    .build());
            paymentRepository.save(payment);
            paymentIds.add(payment.getId());
            return paymentRefundRepository.save(PaymentRefund.requestForWithdrawal(payment, amount, credits, email)).getId();
        });
    }
}
