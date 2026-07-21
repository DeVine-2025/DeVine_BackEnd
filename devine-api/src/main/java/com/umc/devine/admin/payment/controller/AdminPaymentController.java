package com.umc.devine.admin.payment.controller;

import com.umc.devine.admin.payment.dto.AdminPaymentReqDTO;
import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.admin.payment.service.AdminPaymentRefundService;
import com.umc.devine.domain.payment.exception.code.PaymentSuccessCode;
import com.umc.devine.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 어드민 결제 관리 API
 *
 * TODO 인가는 별도로 개발 중인 Member role/시큐리티 체계에 위임
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/v1/payments")
public class AdminPaymentController {

    private final AdminPaymentRefundService adminPaymentRefundService;

    @PostMapping("/{paymentId}/refund")
    public ApiResponse<AdminPaymentResDTO.RefundResultDTO> refund(
            @PathVariable Long paymentId,
            @Valid @RequestBody AdminPaymentReqDTO.RefundDTO request
    ) {
        return ApiResponse.onSuccess(
                PaymentSuccessCode.PAYMENT_REFUNDED,
                adminPaymentRefundService.refund(paymentId, request.reason())
        );
    }
}
