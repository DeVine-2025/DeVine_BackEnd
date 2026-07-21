package com.umc.devine.admin.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminPaymentReqDTO {

    @Schema(description = "환불 요청")
    public record RefundDTO(
            @NotBlank(message = "환불 사유는 필수입니다.")
            @Size(max = 255, message = "환불 사유는 255자를 초과할 수 없습니다.")
            @Schema(description = "환불 사유", example = "고객 요청")
            String reason
    ) {}
}
