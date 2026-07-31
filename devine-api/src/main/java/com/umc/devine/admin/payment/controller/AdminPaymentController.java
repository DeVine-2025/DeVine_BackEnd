package com.umc.devine.admin.payment.controller;

import com.umc.devine.admin.payment.dto.AdminPaymentReqDTO;
import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.admin.payment.service.AdminPaymentQueryService;
import com.umc.devine.admin.payment.service.AdminPaymentRefundService;
import com.umc.devine.domain.payment.exception.code.PaymentSuccessCode;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/v1/payments")
public class AdminPaymentController implements AdminPaymentControllerDocs {

    private final AdminPaymentRefundService adminPaymentRefundService;
    private final AdminPaymentQueryService adminPaymentQueryService;

    @Override
    @GetMapping
    public ApiResponse<PagedResponse<AdminPaymentResDTO.PaymentSummaryDTO>> searchPayments(
            @ParameterObject @ModelAttribute AdminPaymentReqDTO.SearchDTO condition,
            @ParameterObject @ModelAttribute PageRequest pageRequest
    ) {
        return ApiResponse.onSuccess(
                PaymentSuccessCode.PAYMENT_LIST_FOUND,
                adminPaymentQueryService.searchPayments(condition, pageRequest.toPageable())
        );
    }

    @Override
    @GetMapping("/{paymentId}")
    public ApiResponse<AdminPaymentResDTO.PaymentDetailDTO> getPaymentDetail(
            @PathVariable Long paymentId
    ) {
        return ApiResponse.onSuccess(
                PaymentSuccessCode.PAYMENT_DETAIL_FOUND,
                adminPaymentQueryService.getPaymentDetail(paymentId)
        );
    }

    @Override
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
