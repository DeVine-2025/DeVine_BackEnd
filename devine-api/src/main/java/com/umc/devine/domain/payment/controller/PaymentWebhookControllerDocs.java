package com.umc.devine.domain.payment.controller;

import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Payment Webhook", description = "PortOne 결제 웹훅 API")
public interface PaymentWebhookControllerDocs {

    @Operation(summary = "PortOne 웹훅 수신 API", description = "PortOne에서 결제 이벤트 발생 시 호출되는 웹훅 엔드포인트입니다. 서명 검증 후 Transaction.Paid 이벤트만 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 웹훅 처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 본문"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "웹훅 서명 검증 실패")
    })
    ApiResponse<Void> handleWebhook(
            @Parameter(description = "PortOne Webhook ID") @RequestHeader("Webhook-Id") String webhookId,
            @Parameter(description = "PortOne Webhook 타임스탬프 (Unix epoch seconds)") @RequestHeader("Webhook-Timestamp") String webhookTimestamp,
            @Parameter(description = "PortOne Webhook 서명") @RequestHeader("Webhook-Signature") String webhookSignature,
            @Parameter(description = "웹훅 JSON 본문") @RequestBody String rawBody
    );
}
