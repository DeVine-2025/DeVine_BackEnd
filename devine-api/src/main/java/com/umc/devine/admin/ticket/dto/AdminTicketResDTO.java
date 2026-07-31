package com.umc.devine.admin.ticket.dto;

import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public class AdminTicketResDTO {

    @Builder
    @Schema(description = "환불 신청 요약")
    public record RefundRequestDTO(
            @Schema(description = "환불 신청 ID", example = "1")
            Long refundRequestId,

            @Schema(description = "신청자 닉네임", example = "withdrawtarget")
            String memberNickname,

            @Schema(description = "신청 시점 잔여 생성권 수", example = "3")
            int creditAmountAtRequest,

            @Schema(description = "처리 상태", example = "REQUESTED")
            CreditRefundStatus status,

            @Schema(description = "신청일시")
            LocalDateTime requestedAt,

            @Schema(description = "처리일시", nullable = true)
            LocalDateTime processedAt
    ) {}

    @Builder
    @Schema(description = "환불 신청 처리완료 응답")
    public record ProcessRefundRes(
            @Schema(description = "환불 신청 ID", example = "1")
            Long refundRequestId,

            @Schema(description = "처리 상태", example = "PROCESSED")
            CreditRefundStatus status,

            @Schema(description = "처리일시")
            LocalDateTime processedAt
    ) {}
}
