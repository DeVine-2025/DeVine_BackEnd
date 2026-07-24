package com.umc.devine.domain.coupon.controller;

import com.umc.devine.domain.coupon.dto.CouponReqDTO;
import com.umc.devine.domain.coupon.dto.CouponResDTO;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import com.umc.devine.domain.coupon.exception.code.CouponSuccessCode;
import com.umc.devine.domain.coupon.service.command.CouponCommandService;
import com.umc.devine.domain.coupon.service.query.CouponQueryService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.ratelimit.SimpleRateLimiter;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupon")
public class CouponController implements CouponControllerDocs {

    private static final int REGISTER_RATE_LIMIT = 10;
    private static final Duration REGISTER_RATE_WINDOW = Duration.ofMinutes(1);

    private final CouponCommandService couponCommandService;
    private final CouponQueryService couponQueryService;
    private final SimpleRateLimiter rateLimiter;

    @Override
    @PostMapping("/register")
    public ApiResponse<CouponResDTO.MemberCouponDTO> registerByCode(
            @CurrentMember Member member,
            @Valid @RequestBody CouponReqDTO.RegisterCodeReq request
    ) {
        // 쿠폰 코드 브루트포스 시도 방지 — 계정당 분당 10회 (인증된 엔드포인트라 IP보다 memberId 기준이 더 신뢰 가능)
        String rateLimitKey = "rate-limit:coupon-register:" + member.getId();
        if (!rateLimiter.isAllowed(rateLimitKey, REGISTER_RATE_LIMIT, REGISTER_RATE_WINDOW)) {
            throw new CouponException(CouponErrorReason.REGISTER_RATE_LIMIT_EXCEEDED);
        }

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
