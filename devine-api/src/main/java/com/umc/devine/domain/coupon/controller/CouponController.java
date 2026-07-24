package com.umc.devine.domain.coupon.controller;

import com.umc.devine.domain.coupon.dto.CouponReqDTO;
import com.umc.devine.domain.coupon.dto.CouponResDTO;
import com.umc.devine.domain.coupon.exception.code.CouponSuccessCode;
import com.umc.devine.domain.coupon.service.command.CouponCommandService;
import com.umc.devine.domain.coupon.service.query.CouponQueryService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupon")
public class CouponController implements CouponControllerDocs {

    private final CouponCommandService couponCommandService;
    private final CouponQueryService couponQueryService;

    @Override
    @PostMapping("/register")
    public ApiResponse<CouponResDTO.MemberCouponDTO> registerByCode(
            @CurrentMember Member member,
            @Valid @RequestBody CouponReqDTO.RegisterCodeReq request
    ) {
        return ApiResponse.onSuccess(
                CouponSuccessCode.COUPON_REGISTERED,
                couponCommandService.registerByCode(request.code(), member)
        );
    }

    @Override
    @GetMapping("/me")
    public ApiResponse<CouponResDTO.MemberCouponListDTO> getMyCoupons(
            @CurrentMember Member member
    ) {
        return ApiResponse.onSuccess(
                CouponSuccessCode.MY_COUPON_LIST_FOUND,
                couponQueryService.getMyCoupons(member)
        );
    }

    @Override
    @PostMapping("/preview")
    public ApiResponse<CouponResDTO.PreviewDTO> preview(
            @CurrentMember Member member,
            @Valid @RequestBody CouponReqDTO.PreviewReq request
    ) {
        return ApiResponse.onSuccess(
                CouponSuccessCode.PAYMENT_PREVIEW_CALCULATED,
                couponQueryService.preview(request.memberCouponId(), request.originalAmount(), request.ticketProductIds(), member)
        );
    }
}
