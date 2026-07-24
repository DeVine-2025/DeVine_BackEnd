package com.umc.devine.admin.coupon.dto;

import com.umc.devine.domain.coupon.enums.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

public class AdminCouponReqDTO {

    @Schema(description = "쿠폰 발급 방식")
    public enum IssueType {
        ALL,
        SPECIFIC,
        CODE_GEN
    }

    @Schema(description = "쿠폰 생성 요청")
    public record CreateCouponReq(
            @NotNull(message = "쿠폰 이름은 필수입니다.")
            @Schema(description = "쿠폰명/캠페인명", example = "여름 프로모션")
            String name,

            @NotNull(message = "할인 방식은 필수입니다.")
            @Schema(description = "할인 방식 (FIXED_RATE / FIXED_AMOUNT)")
            DiscountType discountType,

            @NotNull(message = "할인 값은 필수입니다.")
            @Positive(message = "할인 값은 양수여야 합니다.")
            @Schema(description = "정률(1~100) 또는 정액(원)", example = "20")
            Long discountValue,

            @Schema(description = "적용 대상 상품 ID (null이면 전체 상품에 적용)", example = "1")
            Long applicableTicketProductId,

            @NotNull(message = "유효기간 시작일은 필수입니다.")
            @Schema(description = "유효기간 시작일")
            LocalDateTime validFrom,

            @NotNull(message = "유효기간 종료일은 필수입니다.")
            @Schema(description = "유효기간 종료일 (시작일 이후여야 함)")
            LocalDateTime validUntil,

            @Positive(message = "발급 수량 제한은 양수여야 합니다.")
            @Schema(description = "발급 수량 제한 (null이면 무제한)", example = "1000")
            Integer totalIssueLimit,

            @Schema(description = "관리자용 메모")
            String description
    ) {}

    @Schema(description = "쿠폰 수정 요청 (null인 필드는 변경하지 않음)")
    public record UpdateCouponReq(
            String name,
            LocalDateTime validFrom,
            LocalDateTime validUntil,

            @Positive(message = "발급 수량 제한은 양수여야 합니다.")
            Integer totalIssueLimit,

            Boolean isActive,
            String description
    ) {}

    @Schema(description = "쿠폰 발급 요청")
    public record IssueCouponReq(
            @NotNull(message = "발급 방식은 필수입니다.")
            IssueType issueType,

            @Schema(description = "SPECIFIC 방식일 때 발급 대상 회원 ID 목록")
            List<Long> memberIds,

            @Schema(description = "CODE_GEN 방식일 때 생성할 코드 자릿수", example = "8")
            Integer codeLength,

            @Schema(description = "CODE_GEN 방식일 때 생성할 코드 개수", example = "100")
            Integer codeCount
    ) {}
}
