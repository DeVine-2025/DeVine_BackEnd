package com.umc.devine.domain.payment.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.converter.PaymentConverter;
import com.umc.devine.domain.payment.dto.PaymentReqDTO;
import com.umc.devine.domain.payment.dto.PaymentResDTO;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.exception.TicketException;
import com.umc.devine.domain.ticket.exception.code.TicketErrorReason;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.domain.ticket.repository.PaymentTicketRepository;
import com.umc.devine.domain.ticket.repository.TicketProductRepository;
import com.umc.devine.infrastructure.portone.PortOneClient;
import com.umc.devine.infrastructure.portone.dto.PortOnePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentCommandServiceImpl implements PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final TransactionTemplate transactionTemplate;
    private final TicketProductRepository ticketProductRepository;
    private final PaymentTicketRepository paymentTicketRepository;
    private final MemberReportCreditRepository memberReportCreditRepository;

    @Override
    public PaymentResDTO.PaymentDTO completePayment(PaymentReqDTO.CompletePaymentDTO request, Member member) {
        // 1. 멱등성 체크
        if (paymentRepository.existsByPortonePaymentId(request.paymentId())) {
            throw new PaymentException(PaymentErrorReason.ALREADY_PROCESSED_PAYMENT);
        }

        // 2. 티켓 상품 유효성 검증 (트랜잭션 밖 — DB 커넥션 점유 방지)
        List<TicketProduct> ticketProducts = request.items().stream()
                .map(item -> {
                    TicketProduct product = ticketProductRepository.findById(item.ticketProductId())
                            .orElseThrow(() -> new TicketException(TicketErrorReason.PRODUCT_NOT_FOUND));
                    if (!product.getActive()) {
                        throw new TicketException(TicketErrorReason.PRODUCT_NOT_ACTIVE);
                    }
                    return product;
                })
                .toList();

        // 3. 서버 측 금액 검증 (상품 가격 × 수량 합계 == 요청 금액)
        long expectedAmount = 0;
        for (int i = 0; i < request.items().size(); i++) {
            expectedAmount += ticketProducts.get(i).getPrice() * request.items().get(i).quantity();
        }
        if (!request.amount().equals(expectedAmount)) {
            throw new PaymentException(PaymentErrorReason.AMOUNT_MISMATCH);
        }

        // 4. PortOne API에서 결제 정보 조회 (트랜잭션 밖 — DB 커넥션 점유 방지)
        PortOnePaymentResponse portOneResponse = portOneClient.getPayment(request.paymentId());

        // 5. 결제 상태 검증
        if (!"PAID".equals(portOneResponse.status())) {
            throw new PaymentException(PaymentErrorReason.PAYMENT_NOT_PAID);
        }

        // 6. 결제 금액 검증 (PortOne 실결제 금액 == 요청 금액)
        if (!request.amount().equals(portOneResponse.amount().total())) {
            throw new PaymentException(PaymentErrorReason.AMOUNT_MISMATCH);
        }

        // 7. MemberReportCredit 행을 미리 보장 (트랜잭션 밖 — 동시 요청 시 UNIQUE 위반 방지)
        ensureCreditRowExists(member);

        // 8. Payment, Transaction, PaymentTicket 저장 및 크레딧 지급 (트랜잭션 안)
        // DataIntegrityViolationException: 동시 요청으로 인한 portone_payment_id unique constraint 위반 방어
        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> {
                Payment payment = PaymentConverter.toPayment(request, member, portOneResponse);
                Transaction transaction = PaymentConverter.toTransaction(portOneResponse, payment);

                attachPaymentDetail(transaction, portOneResponse.method());
                payment.addTransaction(transaction);
                paymentRepository.save(payment);

                // 티켓 구매 내역 저장 및 크레딧 지급
                int totalCredits = 0;
                for (int i = 0; i < request.items().size(); i++) {
                    PaymentReqDTO.TicketPurchaseItem item = request.items().get(i);
                    TicketProduct product = ticketProducts.get(i);

                    PaymentTicket paymentTicket = PaymentTicket.builder()
                            .payment(payment)
                            .ticketProduct(product)
                            .quantity(item.quantity())
                            .unitPrice(product.getPrice())
                            .unitCreditAmount(product.getCreditAmount())
                            .build();
                    paymentTicketRepository.save(paymentTicket);
                    payment.addPaymentTicket(paymentTicket);

                    totalCredits += product.getCreditAmount() * item.quantity();
                }

                grantCredits(member, totalCredits);

                return PaymentConverter.toPaymentDTO(payment);
            }));
        } catch (DataIntegrityViolationException e) {
            throw new PaymentException(PaymentErrorReason.ALREADY_PROCESSED_PAYMENT);
        }
    }

    private void ensureCreditRowExists(Member member) {
        if (memberReportCreditRepository.findByMember(member).isEmpty()) {
            try {
                memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(member, 0));
            } catch (DataIntegrityViolationException ignored) {
                // 동시 요청으로 이미 생성됨 — 무시
            }
        }
    }

    private void grantCredits(Member member, int amount) {
        int updated = memberReportCreditRepository.addCreditsByMember(member, amount);
        if (updated == 0) {
            throw new PaymentException(PaymentErrorReason.CREDIT_UPDATE_FAILED);
        }
    }

    private void attachPaymentDetail(Transaction transaction, PortOnePaymentResponse.MethodDetail method) {
        if (method == null) return;

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.fromPortOneType(method.type());
        } catch (IllegalArgumentException e) {
            throw new PaymentException(PaymentErrorReason.PORTONE_API_ERROR);
        }

        if (paymentMethod == PaymentMethod.CARD) {
            transaction.addCardDetail(PaymentConverter.toCardDetail(method));
        } else if (paymentMethod == PaymentMethod.EASY_PAY) {
            transaction.addEasyPayDetail(PaymentConverter.toEasyPayDetail(method));
        }
    }
}
