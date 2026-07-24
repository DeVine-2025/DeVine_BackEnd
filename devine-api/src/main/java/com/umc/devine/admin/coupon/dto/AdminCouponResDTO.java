package com.umc.devine.admin.coupon.dto;

import com.umc.devine.domain.coupon.enums.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class AdminCouponResDTO {

    @Schema(description = "쿠폰 정보")
    public record CouponDTO(
            Long couponId,
            String name,
            DiscountType discountType,
            Long discountValue,
            Long applicableTicketProductId,
            String applicableTicketProductName,
            Integer totalIssueLimit,
            Integer issuedCount,
            Integer usedCount,
            LocalDateTime validFrom,
            LocalDateTime validUntil,
            Boolean isActive,
            String description,
            LocalDateTime createdAt,

            @Schema(description = "이 쿠폰으로 생성된 코드 목록 (상세 조회에서만 채워지고, 목록 조회에서는 null)")
            List<CouponCodeDTO> codes
    ) {}

    @Schema(description = "쿠폰 코드 정보")
    public record CouponCodeDTO(
            String code,
            Integer maxUses,
            Integer usedCount
    ) {}

    @Schema(description = "쿠폰 발급 결과")
    public record IssueResultDTO(
            @Schema(description = "이번 요청으로 발급된 건수 (ALL/SPECIFIC: 회원 수, CODE_GEN: 생성된 코드 수)")
            int issuedCount,

            @Schema(description = "CODE_GEN 방식일 때 생성된 코드 목록 (그 외에는 null)")
            List<String> generatedCodes
    ) {}

    @Schema(description = "쿠폰 사용 현황")
    public record UsageStatDTO(
            Long couponId,
            String name,
            Integer issuedCount,
            Integer usedCount,
            double usageRate,
            boolean isExpiringSoon,
            LocalDateTime validUntil
    ) {}
}
