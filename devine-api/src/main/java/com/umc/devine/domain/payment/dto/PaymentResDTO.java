package com.umc.devine.domain.payment.dto;

import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.PgProvider;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentResDTO {

    @Builder
    @Schema(description = "결제 정보")
    public record PaymentDTO(
            @Schema(description = "PortOne 결제 ID", example = "payment_1234567890")
            String paymentId,

            @Schema(description = "주문명", example = "테스트 상품")
            String orderName,

            @Schema(description = "결제 금액", example = "10000")
            Long amount,

            @Schema(description = "통화", example = "KRW")
            String currency,

            @Schema(description = "생성 일시")
            LocalDateTime createdAt,

            @Schema(description = "수정 일시")
            LocalDateTime updatedAt,

            @Schema(description = "트랜잭션 목록")
            List<TransactionDTO> transactions
    ) {}

    @Builder
    @Schema(description = "트랜잭션 정보")
    public record TransactionDTO(
            @Schema(description = "PortOne 트랜잭션 ID")
            String transactionId,

            @Schema(description = "트랜잭션 타입")
            TransactionType type,

            @Schema(description = "결제 금액")
            Long amount,

            @Schema(description = "결제 상태")
            TransactionStatus status,

            @Schema(description = "결제 수단")
            PaymentMethod method,

            @Schema(description = "PG사", example = "KCP_V2")
            String pgProvider,

            @Schema(description = "결제 일시")
            LocalDateTime paidAt,

            @Schema(description = "생성 일시")
            LocalDateTime createdAt,

            @Schema(description = "카드 상세 정보", nullable = true)
            CardDetailDTO cardDetail,

            @Schema(description = "간편결제 상세 정보", nullable = true)
            EasyPayDetailDTO easyPayDetail
    ) {}

    @Builder
    @Schema(description = "카드 상세 정보")
    public record CardDetailDTO(
            String cardName,
            String cardNumber,
            String cardBrand,
            String approvalNumber,
            Integer installmentMonth
    ) {}

    @Builder
    @Schema(description = "간편결제 상세 정보")
    public record EasyPayDetailDTO(
            String provider,
            String cardName,
            String cardNumber,
            String cardBrand,
            String approvalNumber,
            Integer installmentMonth
    ) {}

    @Builder
    @Schema(description = "결제 목록")
    public record PaymentListDTO(
            List<PaymentDTO> payments
    ) {}

    @Builder
    @Schema(description = "채널키 조회 응답")
    public record ChannelKeyDTO(
            @Schema(description = "PortOne 채널키")
            String channelKey,

            @Schema(description = "PG사")
            PgProvider pgProvider
    ) {}
}
