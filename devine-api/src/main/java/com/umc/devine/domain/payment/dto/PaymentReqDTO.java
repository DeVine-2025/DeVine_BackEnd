package com.umc.devine.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PaymentReqDTO {

    @Schema(description = "결제 완료 요청")
    public record CompletePaymentDTO(
            @NotBlank(message = "결제 ID는 필수입니다.")
            @Schema(description = "PortOne 결제 ID", example = "payment_1234567890")
            String paymentId,

            @NotBlank(message = "주문명은 필수입니다.")
            @Schema(description = "주문명", example = "테스트 상품")
            String orderName,

            @NotNull(message = "결제 금액은 필수입니다.")
            @Positive(message = "결제 금액은 양수여야 합니다.")
            @Schema(description = "결제 금액 (KRW)", example = "10000")
            Long amount
    ) {}
}
