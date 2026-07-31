package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.infrastructure.portone.PortOneClient;
import com.umc.devine.infrastructure.portone.dto.CancelOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 오케스트레이터의 결과 분기(라우팅)를 검증한다. 핵심: 타임아웃/불명은 절대 FAILED로 종료하지 않는다.
 */
@ExtendWith(MockitoExtension.class)
class AdminPaymentRefundServiceImplTest {

    @Mock
    private RefundTxService tx;
    @Mock
    private PortOneClient portOneClient;

    @InjectMocks
    private AdminPaymentRefundServiceImpl service;

    private static final Long PAYMENT_ID = 1L;
    private static final Long REFUND_ID = 10L;
    private static final String PORTONE_PAYMENT_ID = "payment_123";
    private static final String CANCELLATION_ID = "cancellation_123";
    private static final String REASON = "고객 요청";

    private RefundClaim claim;

    @BeforeEach
    void setUp() {
        claim = new RefundClaim(REFUND_ID, PORTONE_PAYMENT_ID);
    }

    @Test
    @DisplayName("취소 성공이면 complete로 반영")
    void refund_succeeded_settles() {
        given(tx.claim(PAYMENT_ID, REASON)).willReturn(claim);
        given(portOneClient.cancelPayment(PORTONE_PAYMENT_ID, REASON))
                .willReturn(new CancelOutcome.Succeeded(CANCELLATION_ID, 4900L, LocalDateTime.now(), false));
        AdminPaymentResDTO.RefundResultDTO expected = AdminPaymentResDTO.RefundResultDTO.builder()
                .cancellationId(CANCELLATION_ID).amount(4900L).revokedCredits(5).build();
        given(tx.complete(eq(REFUND_ID), any())).willReturn(expected);

        AdminPaymentResDTO.RefundResultDTO result = service.refund(PAYMENT_ID, REASON);

        org.assertj.core.api.Assertions.assertThat(result).isSameAs(expected);
        verify(tx, never()).markFailed(any(), any());
        verify(tx, never()).markUnknown(any(), any(), any());
    }

    @Test
    @DisplayName("PG 거절이면 FAILED로 종료하고 REFUND_REJECTED")
    void refund_rejected_marksFailed() {
        given(tx.claim(PAYMENT_ID, REASON)).willReturn(claim);
        given(portOneClient.cancelPayment(PORTONE_PAYMENT_ID, REASON))
                .willReturn(new CancelOutcome.Rejected("취소 불가 상태"));

        assertThatThrownBy(() -> service.refund(PAYMENT_ID, REASON))
                .isInstanceOf(PaymentException.class)
                .extracting("reason")
                .isEqualTo(PaymentErrorReason.REFUND_REJECTED);

        verify(tx).markFailed(REFUND_ID, "취소 불가 상태");
        verify(tx, never()).complete(any(), any());
    }

    @Test
    @DisplayName("결과 불명이면 UNKNOWN으로 남기고 REFUND_RESULT_UNKNOWN (절대 FAILED 아님)")
    void refund_unknown_marksUnknown() {
        given(tx.claim(PAYMENT_ID, REASON)).willReturn(claim);
        given(portOneClient.cancelPayment(PORTONE_PAYMENT_ID, REASON))
                .willReturn(new CancelOutcome.Unknown("타임아웃"));

        assertThatThrownBy(() -> service.refund(PAYMENT_ID, REASON))
                .isInstanceOf(PaymentException.class)
                .extracting("reason")
                .isEqualTo(PaymentErrorReason.REFUND_RESULT_UNKNOWN);

        verify(tx).markUnknown(REFUND_ID, null, "타임아웃");
        verify(tx, never()).markFailed(any(), any());
    }

    @Test
    @DisplayName("PG 취소 성공했으나 DB 반영 실패면 UNKNOWN(취소 id 보존) + REFUND_SETTLEMENT_FAILED")
    void refund_settlementFailure_marksUnknownWithCancellationId() {
        given(tx.claim(PAYMENT_ID, REASON)).willReturn(claim);
        given(portOneClient.cancelPayment(PORTONE_PAYMENT_ID, REASON))
                .willReturn(new CancelOutcome.Succeeded(CANCELLATION_ID, 4900L, LocalDateTime.now(), false));
        willThrow(new RuntimeException("DB error")).given(tx).complete(eq(REFUND_ID), any());

        assertThatThrownBy(() -> service.refund(PAYMENT_ID, REASON))
                .isInstanceOf(PaymentException.class)
                .extracting("reason")
                .isEqualTo(PaymentErrorReason.REFUND_SETTLEMENT_FAILED);

        // 취소 id를 보존한 채 UNKNOWN — 대사가 복구할 수 있게
        verify(tx).markUnknown(eq(REFUND_ID), eq(CANCELLATION_ID), any());
        verify(tx, never()).markFailed(any(), any());
    }

    @Test
    @DisplayName("이미 환불(진행) 중이면 claim에서 막고 PortOne 호출 없음")
    void refund_alreadyRefunded_blockedAtClaim() {
        given(tx.claim(PAYMENT_ID, REASON))
                .willThrow(new PaymentException(PaymentErrorReason.PAYMENT_ALREADY_REFUNDED));

        assertThatThrownBy(() -> service.refund(PAYMENT_ID, REASON))
                .isInstanceOf(PaymentException.class)
                .extracting("reason")
                .isEqualTo(PaymentErrorReason.PAYMENT_ALREADY_REFUNDED);

        verify(portOneClient, never()).cancelPayment(any(), any());
    }
}
