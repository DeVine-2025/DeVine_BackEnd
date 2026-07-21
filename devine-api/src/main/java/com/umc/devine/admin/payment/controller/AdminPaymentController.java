package com.umc.devine.admin.payment.controller;

import com.umc.devine.admin.payment.dto.AdminPaymentReqDTO;
import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.admin.payment.service.AdminPaymentQueryService;
import com.umc.devine.admin.payment.service.AdminPaymentRefundService;
import com.umc.devine.domain.payment.exception.code.PaymentSuccessCode;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 어드민 결제 관리 API
 *
 * 공개 Swagger 문서에서 제외한다(@Hidden). 명세는 docs/api/admin-payment-api.md 참고.
 *
 * TODO 인가는 별도로 개발 중인 Member role/시큐리티 체계에 위임
 */
@Hidden
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/v1/payments")
public class AdminPaymentController {

    private final AdminPaymentRefundService adminPaymentRefundService;
    private final AdminPaymentQueryService adminPaymentQueryService;

    @GetMapping
    public ApiResponse<PagedResponse<AdminPaymentResDTO.PaymentSummaryDTO>> searchPayments(
            @ModelAttribute AdminPaymentReqDTO.SearchDTO condition,
            @ModelAttribute PageRequest pageRequest
    ) {
        return ApiResponse.onSuccess(
                PaymentSuccessCode.PAYMENT_LIST_FOUND,
                adminPaymentQueryService.searchPayments(condition, pageRequest.toPageable())
        );
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<AdminPaymentResDTO.PaymentDetailDTO> getPaymentDetail(
            @PathVariable Long paymentId
    ) {
        return ApiResponse.onSuccess(
                PaymentSuccessCode.PAYMENT_DETAIL_FOUND,
                adminPaymentQueryService.getPaymentDetail(paymentId)
        );
    }

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
