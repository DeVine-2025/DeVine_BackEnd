package com.umc.devine.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.domain.payment.service.command.PaymentCommandService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.infrastructure.portone.PortOneWebhookVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 웹훅 컨트롤러가 PortOne 호출 실패(재시도 가능)와 그 외 비즈니스 오류(재시도 불가)를
 * 올바르게 구분해서 처리하는지 검증한다. 서명 검증까지 실제 로직을 태우기 위해
 * PortOneWebhookVerifier는 Mock 대신 테스트 시크릿으로 만든 실제 인스턴스를 사용한다.
 */
class PaymentWebhookControllerTest {

    private static final byte[] TEST_SECRET = "test-webhook-secret-key!".getBytes(StandardCharsets.UTF_8);
    private static final String TEST_WEBHOOK_SECRET = "whsec_" + Base64.getEncoder().encodeToString(TEST_SECRET);
    private static final String PAYMENT_ID = "payment_test_1";

    private final PaymentCommandService paymentCommandService = mock(PaymentCommandService.class);
    private final PortOneWebhookVerifier webhookVerifier = new PortOneWebhookVerifier(TEST_WEBHOOK_SECRET);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentWebhookController controller =
            new PaymentWebhookController(paymentCommandService, webhookVerifier, objectMapper);

    private String webhookId;
    private String timestamp;
    private String body;
    private String signature;

    @BeforeEach
    void setUp() throws Exception {
        webhookId = "msg_test";
        timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        body = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"" + PAYMENT_ID + "\"}}";
        signature = generateSignature(webhookId, timestamp, body);
    }

    @Test
    @DisplayName("PortOne 호출 실패(PORTONE_API_ERROR)는 예외를 다시 던져 PortOne 재시도를 유도한다")
    void retryableError_isRethrown() {
        willThrow(new PaymentException(PaymentErrorReason.PORTONE_API_ERROR))
                .given(paymentCommandService).handleWebhookPayment(PAYMENT_ID);

        assertThatThrownBy(() -> controller.handleWebhook(webhookId, timestamp, signature, body))
                .isInstanceOf(PaymentException.class)
                .satisfies(e -> assertThat(((PaymentException) e).getReason())
                        .isEqualTo(PaymentErrorReason.PORTONE_API_ERROR));
    }

    @Test
    @DisplayName("재시도 불가능한 PaymentException은 로그만 남기고 200을 반환한다")
    void nonRetryablePaymentError_isSwallowed() {
        willThrow(new PaymentException(PaymentErrorReason.CREDIT_UPDATE_FAILED))
                .given(paymentCommandService).handleWebhookPayment(PAYMENT_ID);

        ApiResponse<Void> response = controller.handleWebhook(webhookId, timestamp, signature, body);

        assertThat(response.getIsSuccess()).isTrue();
        verify(paymentCommandService).handleWebhookPayment(PAYMENT_ID);
    }

    @Test
    @DisplayName("쿠폰 관련 비즈니스 오류(CouponException)도 로그만 남기고 200을 반환한다")
    void couponBusinessError_isSwallowed() {
        willThrow(new CouponException(CouponErrorReason.COUPON_ALREADY_USED))
                .given(paymentCommandService).handleWebhookPayment(PAYMENT_ID);

        ApiResponse<Void> response = controller.handleWebhook(webhookId, timestamp, signature, body);

        assertThat(response.getIsSuccess()).isTrue();
    }

    @Test
    @DisplayName("서명이 유효하지 않으면 서비스 호출 없이 예외를 던진다")
    void invalidSignature_throwsBeforeCallingService() {
        assertThatThrownBy(() -> controller.handleWebhook(webhookId, timestamp, "v1,invalid==", body))
                .isInstanceOf(PaymentException.class)
                .satisfies(e -> assertThat(((PaymentException) e).getReason())
                        .isEqualTo(PaymentErrorReason.INVALID_WEBHOOK_SIGNATURE));
    }

    private String generateSignature(String webhookId, String timestamp, String body) throws Exception {
        String signedContent = webhookId + "." + timestamp + "." + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET, "HmacSHA256"));
        byte[] hash = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
        return "v1," + Base64.getEncoder().encodeToString(hash);
    }
}
