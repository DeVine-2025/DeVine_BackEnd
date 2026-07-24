package com.umc.devine.admin.coupon.controller;

import com.umc.devine.admin.coupon.dto.AdminCouponReqDTO;
import com.umc.devine.admin.coupon.dto.AdminCouponResDTO;
import com.umc.devine.admin.coupon.exception.code.AdminCouponSuccessCode;
import com.umc.devine.admin.coupon.service.command.AdminCouponCommandService;
import com.umc.devine.admin.coupon.service.query.AdminCouponQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.dto.PageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/v1/coupon")
public class AdminCouponController implements AdminCouponControllerDocs {

    private final AdminCouponCommandService adminCouponCommandService;
    private final AdminCouponQueryService adminCouponQueryService;

    @Override
    @PostMapping
    public ApiResponse<AdminCouponResDTO.CouponDTO> createCoupon(
            @Valid @RequestBody AdminCouponReqDTO.CreateCouponReq request
    ) {
        return ApiResponse.onSuccess(
                AdminCouponSuccessCode.COUPON_CREATED,
                adminCouponCommandService.createCoupon(request)
        );
    }

    @Override
    @GetMapping
    public ApiResponse<PagedResponse<AdminCouponResDTO.CouponDTO>> getCoupons(
            @ParameterObject @ModelAttribute PageRequest pageRequest
    ) {
        return ApiResponse.onSuccess(
                AdminCouponSuccessCode.COUPON_LIST_FOUND,
                adminCouponQueryService.getCoupons(pageRequest)
        );
    }

    @Override
    @PatchMapping("/{couponId}")
    public ApiResponse<AdminCouponResDTO.CouponDTO> updateCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody AdminCouponReqDTO.UpdateCouponReq request
    ) {
        return ApiResponse.onSuccess(
                AdminCouponSuccessCode.COUPON_UPDATED,
                adminCouponCommandService.updateCoupon(couponId, request)
        );
    }

    @Override
    @PostMapping("/{couponId}/issue")
    public ApiResponse<AdminCouponResDTO.IssueResultDTO> issueCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody AdminCouponReqDTO.IssueCouponReq request
    ) {
        return ApiResponse.onSuccess(
                AdminCouponSuccessCode.COUPON_ISSUED,
                adminCouponCommandService.issueCoupon(couponId, request)
        );
    }

    @Override
    @GetMapping("/usage")
    public ApiResponse<List<AdminCouponResDTO.UsageStatDTO>> getUsageStats(
            @RequestParam(required = false) Long couponId
    ) {
        return ApiResponse.onSuccess(
                AdminCouponSuccessCode.COUPON_USAGE_FOUND,
                adminCouponQueryService.getUsageStats(couponId)
        );
    }
}
