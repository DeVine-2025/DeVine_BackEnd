package com.umc.devine.domain.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class CouponReqDTO {

    @Schema(description = "쿠폰 코드 등록 요청")
    public record RegisterCodeReq(
            @NotBlank(message = "쿠폰 코드는 필수입니다.")
            @Schema(description = "쿠폰 코드", example = "A1B2C3D4")
            String code
    ) {}

    @Schema(description = "결제 할인 미리보기 요청")
    public record PreviewReq(
            @NotNull(message = "보유 쿠폰 ID는 필수입니다.")
            @Schema(description = "적용할 보유 쿠폰(memberCoupon) ID", example = "1")
            Long memberCouponId,

            @NotNull(message = "원래 결제 금액은 필수입니다.")
            @Positive(message = "결제 금액은 양수여야 합니다.")
            @Schema(description = "쿠폰 적용 전 원래 결제 금액", example = "9900")
            Long originalAmount,

            @Schema(description = "구매하려는 티켓 상품 ID 목록 (쿠폰의 적용 대상 상품 검증용)")
            List<Long> ticketProductIds
    ) {}
}
