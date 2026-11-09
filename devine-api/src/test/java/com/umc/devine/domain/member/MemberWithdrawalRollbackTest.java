package com.umc.devine.domain.member;

import com.umc.devine.domain.member.dto.WithdrawalReqDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.enums.WithdrawalReason;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.WithdrawalReasonStatRepository;
import com.umc.devine.domain.member.service.command.MemberWithdrawalCommandService;
import com.umc.devine.domain.outbox.repository.OutboxTaskRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.domain.ticket.repository.TicketProductRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * T14: 탈퇴 처리 중 DB 오류가 나면 전체 롤백된다. 환불 신청 행도, 아웃박스 작업(운영자 메일/외부 정리)도 없고 회원 상태는 그대로다.
 * 롤백 여부를 실제 커밋 기준으로 보려고 테스트 트랜잭션을 쓰지 않고, 끝나면 직접 지운다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberWithdrawalRollbackTest extends IntegrationTestSupport {

    @Autowired private MemberWithdrawalCommandService withdrawalService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private MemberReportCreditRepository memberReportCreditRepository;
    @Autowired private TicketProductRepository ticketProductRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentRefundRepository paymentRefundRepository;
    @Autowired private OutboxTaskRepository outboxTaskRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    // 탈퇴 트랜잭션의 거의 마지막 쓰기(탈퇴 사유 통계)에서 DB 오류를 낸다. 그 앞의 삭제, 익명화, 환불 신청이 모두 롤백돼야 한다
    @MockitoBean private WithdrawalReasonStatRepository withdrawalReasonStatRepository;

    private Long memberId;
    private Long paymentId;
    private Long productId;

    @AfterEach
    void cleanUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            outboxTaskRepository.deleteAll();
            paymentRefundRepository.findAllByPaymentIdIn(List.of(paymentId)).forEach(paymentRefundRepository::delete);
            paymentRepository.deleteById(paymentId);
            ticketProductRepository.deleteById(productId);
            memberRepository.findById(memberId).ifPresent(member -> {
                contactRepository.deleteAllByMember(member);
                memberReportCreditRepository.findByMember(member).ifPresent(memberReportCreditRepository::delete);
                memberRepository.delete(member);
            });
        });
    }

    @Test
    @DisplayName("T14: 탈퇴 처리 중 DB 오류가 나면 전체 롤백되고 아무것도 바뀌지 않는다")
    void rollsBackEverything() {
        Member member = new TransactionTemplate(transactionManager).execute(status -> setUpMember());
        given(withdrawalReasonStatRepository.save(any()))
                .willThrow(new DataIntegrityViolationException("강제 DB 오류"));

        assertThatThrownBy(() -> withdrawalService.withdraw(member, WithdrawalReqDTO.WithdrawReq.builder()
                .confirmed(true).refundRequested(true).refundContactEmail("refund@example.com")
                .reason(WithdrawalReason.OTHER).build()))
                .isInstanceOf(DataIntegrityViolationException.class);

        Member reloaded = memberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getUsed()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(reloaded.getClerkId()).isEqualTo("clerk_rb_member");
        assertThat(reloaded.getNickname()).isEqualTo("rbmember");
        assertThat(contactRepository.findAllByMember(reloaded)).hasSize(1);
        assertThat(memberReportCreditRepository.findByMember(reloaded).orElseThrow().getRemainingCount()).isEqualTo(1);
        assertThat(paymentRefundRepository.findAllByPaymentIdIn(List.of(paymentId))).isEmpty();
        assertThat(outboxTaskRepository.findAll()).isEmpty();
    }

    private Member setUpMember() {
        Member member = memberRepository.save(Member.builder()
                .clerkId("clerk_rb_member").nickname("rbmember")
                .mainType(MemberMainType.DEVELOPER).used(MemberStatus.ACTIVE).build());
        memberId = member.getId();
        contactRepository.save(Contact.builder().member(member).contactType(ContactType.EMAIL).value("rb@example.com").build());
        memberReportCreditRepository.save(MemberReportCredit.of(member, 1));

        TicketProduct product = ticketProductRepository.save(TicketProduct.builder()
                .name("리포트 생성권 롤백").price(4_900L).creditAmount(1).active(true).build());
        productId = product.getId();
        Payment payment = Payment.builder()
                .portonePaymentId("pay_rb").member(member).orderName(product.getName())
                .amount(4_900L).currency("KRW").build();
        payment.addTransaction(Transaction.builder()
                .portoneTransactionId("tx_rb").payment(payment)
                .type(TransactionType.PAYMENT).status(TransactionStatus.PAID)
                .method(PaymentMethod.CARD).pgProvider("TOSSPAYMENTS")
                .amount(4_900L).paidAt(LocalDateTime.now().minusDays(1)).build());
        payment.addPaymentTicket(PaymentTicket.builder()
                .payment(payment).ticketProduct(product).quantity(1)
                .unitPrice(product.getPrice()).unitCreditAmount(1).build());
        paymentId = paymentRepository.save(payment).getId();
        return member;
    }
}
