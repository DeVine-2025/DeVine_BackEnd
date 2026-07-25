package com.umc.devine.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public class PaymentReqDTO {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookDTO(
            String type,
            Data data
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Data(String paymentId) {}
    }

    @Schema(description = "PortOne customData에 담기는 주문 정보 (프론트가 결제 요청 시 포함)")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookCustomDataDTO(
            String clerkId,
            String orderName,
            Long memberCouponId,
            List<TicketPurchaseItem> items
    ) {}


    @Schema(description = "결제 완료 요청")
    public record CompletePaymentDTO(
            @NotBlank(message = "결제 ID는 필수입니다.")
            @Size(max = 40, message = "결제 ID는 40자를 초과할 수 없습니다.")
            @Schema(description = "PortOne 결제 ID", example = "payment_1234567890")
            String paymentId,

            @NotBlank(message = "주문명은 필수입니다.")
            @Schema(description = "주문명", example = "리포트 생성권 1개 x1")
            String orderName,

            @NotNull(message = "결제 금액은 필수입니다.")
            @Positive(message = "결제 금액은 양수여야 합니다.")
            @Schema(description = "결제 금액 (KRW)", example = "4900")
            Long amount,

            @NotEmpty(message = "구매 항목은 1개 이상이어야 합니다.")
            @Valid
            @Schema(description = "구매 항목 목록")
            List<TicketPurchaseItem> items,

            @Schema(description = "적용할 보유 쿠폰(memberCoupon) ID (없으면 null)")
            Long memberCouponId
    ) {}

    @Schema(description = "0원 결제 요청 (쿠폰 할인으로 결제 금액이 0원이 되는 경우, PortOne 호출 없이 서버에서 직접 처리)")
    public record FreePaymentDTO(
            @NotBlank(message = "주문명은 필수입니다.")
            @Schema(description = "주문명", example = "리포트 생성권 1개 x1")
            String orderName,

            @NotNull(message = "원래 결제 금액은 필수입니다.")
            @Positive(message = "결제 금액은 양수여야 합니다.")
            @Schema(description = "쿠폰 적용 전 원래 결제 금액 (KRW)", example = "4900")
            Long originalAmount,

            @NotEmpty(message = "구매 항목은 1개 이상이어야 합니다.")
            @Valid
            @Schema(description = "구매 항목 목록")
            List<TicketPurchaseItem> items,

            @NotNull(message = "0원 결제는 쿠폰이 필수입니다.")
            @Schema(description = "적용할 보유 쿠폰(memberCoupon) ID")
            Long memberCouponId
    ) {}

    @Schema(description = "구매 항목")
    public record TicketPurchaseItem(
            @NotNull(message = "티켓 상품 ID는 필수입니다.")
            @Schema(description = "티켓 상품 ID", example = "1")
            Long ticketProductId,

            @NotNull(message = "수량은 필수입니다.")
            @Positive(message = "수량은 1개 이상이어야 합니다.")
            @Schema(description = "구매 수량", example = "5")
            Integer quantity
    ) {}
}
