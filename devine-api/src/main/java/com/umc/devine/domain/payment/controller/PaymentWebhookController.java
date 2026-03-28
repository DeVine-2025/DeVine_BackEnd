package com.umc.devine.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.payment.dto.PaymentReqDTO;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.domain.payment.exception.code.PaymentSuccessCode;
import com.umc.devine.domain.payment.service.command.PaymentCommandService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.infrastructure.portone.PortOneWebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentWebhookController implements PaymentWebhookControllerDocs {

    private final PaymentCommandService paymentCommandService;
    private final PortOneWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;

    @Override
    @PostMapping("/webhook")
    public ApiResponse<Void> handleWebhook(
            @RequestHeader("Webhook-Id") String webhookId,
            @RequestHeader("Webhook-Timestamp") String webhookTimestamp,
            @RequestHeader("Webhook-Signature") String webhookSignature,
            @RequestBody String rawBody
    ) {
        // 1. 서명 검증
        if (!webhookVerifier.verify(webhookId, webhookTimestamp, webhookSignature, rawBody)) {
            throw new PaymentException(PaymentErrorReason.INVALID_WEBHOOK_SIGNATURE);
        }

        // 2. 본문 파싱
        PaymentReqDTO.WebhookDTO webhook;
        try {
            webhook = objectMapper.readValue(rawBody, PaymentReqDTO.WebhookDTO.class);
        } catch (Exception e) {
            throw new PaymentException(PaymentErrorReason.INVALID_WEBHOOK_BODY);
        }

        // 3. Transaction.Paid 이벤트만 처리
        if (!"Transaction.Paid".equals(webhook.type())) {
            log.info("웹훅: 처리하지 않는 이벤트 타입 - type: {}", webhook.type());
            return ApiResponse.onSuccess(PaymentSuccessCode.WEBHOOK_PROCESSED, null);
        }

        if (webhook.data() == null || webhook.data().paymentId() == null) {
            throw new PaymentException(PaymentErrorReason.INVALID_WEBHOOK_BODY);
        }

        // 4. 결제 처리 — 비즈니스 오류(PaymentException)는 200 반환, 일시적 오류는 전파하여 PortOne 재시도 유도
        try {
            paymentCommandService.handleWebhookPayment(webhook.data().paymentId());
        } catch (PaymentException e) {
            log.warn("웹훅: 결제 처리 비즈니스 오류 - paymentId: {}, error: {}", webhook.data().paymentId(), e.getMessage());
        }

        return ApiResponse.onSuccess(PaymentSuccessCode.WEBHOOK_PROCESSED, null);
    }
}
