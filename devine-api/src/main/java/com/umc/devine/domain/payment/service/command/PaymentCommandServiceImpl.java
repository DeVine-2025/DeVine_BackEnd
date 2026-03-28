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
import com.umc.devine.infrastructure.portone.PortOneClient;
import com.umc.devine.infrastructure.portone.dto.PortOnePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentCommandServiceImpl implements PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    public PaymentResDTO.PaymentDTO completePayment(PaymentReqDTO.CompletePaymentDTO request, Member member) {
        // 1. 멱등성 체크
        if (paymentRepository.existsByPortonePaymentId(request.paymentId())) {
            throw new PaymentException(PaymentErrorReason.ALREADY_PROCESSED_PAYMENT);
        }

        // 2. PortOne API에서 결제 정보 조회 (트랜잭션 밖 — DB 커넥션 점유 방지)
        PortOnePaymentResponse portOneResponse = portOneClient.getPayment(request.paymentId());

        // 3. 결제 상태 검증
        if (!"PAID".equals(portOneResponse.status())) {
            throw new PaymentException(PaymentErrorReason.PAYMENT_NOT_PAID);
        }

        // 4. 결제 금액 검증
        if (!request.amount().equals(portOneResponse.amount().total())) {
            throw new PaymentException(PaymentErrorReason.AMOUNT_MISMATCH);
        }

        // 5. Payment, Transaction, 상세 정보 저장 (트랜잭션 안)
        // DataIntegrityViolationException: 동시 요청으로 인한 unique constraint 위반 방어
        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> {
                Payment payment = PaymentConverter.toPayment(request, member, portOneResponse);
                Transaction transaction = PaymentConverter.toTransaction(portOneResponse, payment);

                attachPaymentDetail(transaction, portOneResponse.method());

                payment.addTransaction(transaction);
                paymentRepository.save(payment);

                return PaymentConverter.toPaymentDTO(payment);
            }));
        } catch (DataIntegrityViolationException e) {
            throw new PaymentException(PaymentErrorReason.ALREADY_PROCESSED_PAYMENT);
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
