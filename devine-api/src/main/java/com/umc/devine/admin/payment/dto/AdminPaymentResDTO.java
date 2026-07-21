package com.umc.devine.admin.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public class AdminPaymentResDTO {

    @Schema(description = "환불 결과")
    @Builder
    public record RefundResultDTO(
            @Schema(description = "PortOne 취소 id", example = "cancellation_1234567890")
            String cancellationId,

            @Schema(description = "환불 금액 (KRW)", example = "4900")
            Long amount,

            @Schema(description = "취소 완료 시각")
            LocalDateTime cancelledAt,

            @Schema(description = "회수된 크레딧 수", example = "5")
            int revokedCredits
    ) {}
}
