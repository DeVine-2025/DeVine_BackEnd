package com.umc.devine.domain.payment.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.dto.PaymentReqDTO;
import com.umc.devine.domain.payment.dto.PaymentResDTO;
import com.umc.devine.domain.payment.enums.PgProvider;
import com.umc.devine.domain.payment.exception.code.PaymentSuccessCode;
import com.umc.devine.domain.payment.service.command.PaymentCommandService;
import com.umc.devine.domain.payment.service.query.PaymentQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController implements PaymentControllerDocs {

    private final PaymentCommandService paymentCommandService;
    private final PaymentQueryService paymentQueryService;

    @Override
    @PostMapping("/complete")
    public ApiResponse<PaymentResDTO.PaymentDTO> completePayment(
            @CurrentMember Member member,
            @Valid @RequestBody PaymentReqDTO.CompletePaymentDTO request
    ) {
        return ApiResponse.onSuccess(
                PaymentSuccessCode.PAYMENT_COMPLETED,
                paymentCommandService.completePayment(request, member)
        );
    }

    @Override
    @GetMapping("/my")
    public ApiResponse<PaymentResDTO.PaymentListDTO> getMyPayments(
            @CurrentMember Member member
    ) {
        return ApiResponse.onSuccess(
                PaymentSuccessCode.PAYMENT_LIST_FOUND,
                paymentQueryService.getMyPayments(member)
        );
    }

    @Override
    @GetMapping("/channel-key")
    public ApiResponse<PaymentResDTO.ChannelKeyDTO> getChannelKey(
            @RequestParam PgProvider pg
    ) {
        return ApiResponse.onSuccess(
                PaymentSuccessCode.CHANNEL_KEY_FOUND,
                paymentQueryService.getChannelKey(pg)
        );
    }
}
