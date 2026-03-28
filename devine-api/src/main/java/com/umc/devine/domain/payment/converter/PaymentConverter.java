package com.umc.devine.domain.payment.converter;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.dto.PaymentReqDTO;
import com.umc.devine.domain.payment.dto.PaymentResDTO;
import com.umc.devine.domain.payment.entity.CardDetail;
import com.umc.devine.domain.payment.entity.EasyPayDetail;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.infrastructure.portone.dto.PortOnePaymentResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public class PaymentConverter {

    public static Payment toPayment(PaymentReqDTO.CompletePaymentDTO request, Member member, PortOnePaymentResponse portOneResponse) {
        return Payment.builder()
                .portonePaymentId(request.paymentId())
                .member(member)
                .orderName(request.orderName())
                .amount(portOneResponse.amount().total())
                .currency(portOneResponse.currency())
                .build();
    }

    public static Transaction toTransaction(PortOnePaymentResponse portOneResponse, Payment payment) {
        return Transaction.builder()
                .portoneTransactionId(portOneResponse.transactionId())
                .payment(payment)
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PAID)
                .method(PaymentMethod.fromPortOneType(portOneResponse.method().type()))
                .pgProvider(portOneResponse.pgProvider() != null ? portOneResponse.pgProvider() : "UNKNOWN")
                .amount(portOneResponse.amount().total())
                .paidAt(parseDateTime(portOneResponse.paidAt()))
                .build();
    }

    public static CardDetail toCardDetail(PortOnePaymentResponse.MethodDetail method) {
        PortOnePaymentResponse.CardInfo card = method.card();
        return CardDetail.builder()
                .cardName(card != null ? card.name() : null)
                .cardNumber(card != null ? card.number() : null)
                .cardBrand(card != null ? card.brand() : null)
                .approvalNumber(method.approvalNumber())
                .installmentMonth(method.installment() != null ? method.installment().month() : null)
                .build();
    }

    public static EasyPayDetail toEasyPayDetail(PortOnePaymentResponse.MethodDetail method) {
        PortOnePaymentResponse.EasyPayMethodInfo easyPayMethod = method.easyPayMethod();
        PortOnePaymentResponse.CardInfo card = easyPayMethod != null ? easyPayMethod.card() : null;
        return EasyPayDetail.builder()
                .provider(method.provider())
                .cardName(card != null ? card.name() : null)
                .cardNumber(card != null ? card.number() : null)
                .cardBrand(card != null ? card.brand() : null)
                .approvalNumber(easyPayMethod != null ? easyPayMethod.approvalNumber() : null)
                .installmentMonth(easyPayMethod != null && easyPayMethod.installment() != null
                        ? easyPayMethod.installment().month() : null)
                .build();
    }

    public static PaymentResDTO.PaymentDTO toPaymentDTO(Payment payment) {
        List<PaymentResDTO.TransactionDTO> transactionDTOs = payment.getTransactions().stream()
                .map(PaymentConverter::toTransactionDTO)
                .toList();

        return PaymentResDTO.PaymentDTO.builder()
                .paymentId(payment.getPortonePaymentId())
                .orderName(payment.getOrderName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .transactions(transactionDTOs)
                .build();
    }

    public static PaymentResDTO.PaymentListDTO toPaymentListDTO(List<Payment> payments) {
        return PaymentResDTO.PaymentListDTO.builder()
                .payments(payments.stream().map(PaymentConverter::toPaymentDTO).toList())
                .build();
    }

    private static PaymentResDTO.TransactionDTO toTransactionDTO(Transaction transaction) {
        return PaymentResDTO.TransactionDTO.builder()
                .transactionId(transaction.getPortoneTransactionId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .method(transaction.getMethod())
                .pgProvider(transaction.getPgProvider())
                .paidAt(transaction.getPaidAt())
                .createdAt(transaction.getCreatedAt())
                .cardDetail(transaction.getCardDetail() != null ? toCardDetailDTO(transaction.getCardDetail()) : null)
                .easyPayDetail(transaction.getEasyPayDetail() != null ? toEasyPayDetailDTO(transaction.getEasyPayDetail()) : null)
                .build();
    }

    private static PaymentResDTO.CardDetailDTO toCardDetailDTO(CardDetail cardDetail) {
        return PaymentResDTO.CardDetailDTO.builder()
                .cardName(cardDetail.getCardName())
                .cardNumber(cardDetail.getCardNumber())
                .cardBrand(cardDetail.getCardBrand())
                .approvalNumber(cardDetail.getApprovalNumber())
                .installmentMonth(cardDetail.getInstallmentMonth())
                .build();
    }

    private static PaymentResDTO.EasyPayDetailDTO toEasyPayDetailDTO(EasyPayDetail easyPayDetail) {
        return PaymentResDTO.EasyPayDetailDTO.builder()
                .provider(easyPayDetail.getProvider())
                .cardName(easyPayDetail.getCardName())
                .cardNumber(easyPayDetail.getCardNumber())
                .cardBrand(easyPayDetail.getCardBrand())
                .approvalNumber(easyPayDetail.getApprovalNumber())
                .installmentMonth(easyPayDetail.getInstallmentMonth())
                .build();
    }

    private static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return null;
        return OffsetDateTime.parse(dateTimeStr).toLocalDateTime();
    }
}
