package com.umc.devine.admin.payment.dto;

import com.umc.devine.admin.payment.enums.AdminPaymentStatus;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

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

    @Schema(description = "결제 목록 항목")
    @Builder
    public record PaymentSummaryDTO(
            @Schema(description = "결제 ID", example = "1")
            Long paymentId,

            @Schema(description = "유저 ID", example = "1")
            Long memberId,

            @Schema(description = "유저 닉네임", example = "홍길동")
            String memberNickname,

            @Schema(description = "주문명", example = "리포트 생성권 3개 묶음")
            String orderName,

            @Schema(description = "결제 금액 (KRW)", example = "12000")
            Long amount,

            @Schema(description = "결제 일시")
            LocalDateTime paidAt,

            @Schema(description = "결제 상태")
            AdminPaymentStatus status
    ) {}

    @Schema(description = "결제 상세")
    @Builder
    public record PaymentDetailDTO(
            @Schema(description = "결제 ID", example = "1")
            Long paymentId,

            @Schema(description = "PortOne 결제 ID", example = "payment_1234567890")
            String portonePaymentId,

            @Schema(description = "유저 ID", example = "1")
            Long memberId,

            @Schema(description = "유저 닉네임", example = "홍길동")
            String memberNickname,

            @Schema(description = "주문명", example = "리포트 생성권 3개 묶음")
            String orderName,

            @Schema(description = "결제 금액", example = "12000")
            Long amount,

            @Schema(description = "통화", example = "KRW")
            String currency,

            @Schema(description = "결제 일시")
            LocalDateTime paidAt,

            @Schema(description = "결제 상태")
            AdminPaymentStatus status,

            @Schema(description = "결제 수단 상세", nullable = true)
            PaymentMethodDTO method,

            @Schema(description = "PG사", example = "TOSSPAYMENTS")
            String pgProvider,

            @Schema(description = "구매 상품 목록")
            List<TicketDTO> tickets,

            @Schema(description = "해당 유저의 현재 잔여 리포트 생성권 수", example = "3")
            int remainingReportCredits,

            @Schema(description = "환불 정보 (이력이 있을 때만)", nullable = true)
            RefundDTO refund
    ) {}

    @Schema(description = "결제 수단 상세")
    @Builder
    public record PaymentMethodDTO(
            @Schema(description = "결제 수단")
            PaymentMethod method,

            @Schema(description = "간편결제사 (간편결제인 경우)", nullable = true)
            String provider,

            @Schema(description = "카드사", nullable = true)
            String cardName,

            @Schema(description = "카드 번호", nullable = true)
            String cardNumber,

            @Schema(description = "카드 브랜드", nullable = true)
            String cardBrand,

            @Schema(description = "승인 번호", nullable = true)
            String approvalNumber,

            @Schema(description = "할부 개월", nullable = true)
            Integer installmentMonth
    ) {}

    @Schema(description = "구매 상품")
    @Builder
    public record TicketDTO(
            @Schema(description = "상품 ID", example = "2")
            Long ticketProductId,

            @Schema(description = "상품명", example = "리포트 생성권 3개 묶음")
            String productName,

            @Schema(description = "수량", example = "1")
            Integer quantity,

            @Schema(description = "단가", example = "12000")
            Long unitPrice,

            @Schema(description = "단위당 크레딧 수", example = "3")
            Integer unitCreditAmount,

            @Schema(description = "이 결제로 지급된 총 크레딧 수", example = "3")
            int totalCredits
    ) {}

    @Schema(description = "환불 정보")
    @Builder
    public record RefundDTO(
            @Schema(description = "환불 상태")
            RefundStatus status,

            @Schema(description = "환불 사유", example = "고객 요청")
            String reason,

            @Schema(description = "PortOne 취소 id", nullable = true)
            String cancellationId,

            @Schema(description = "실패 사유", nullable = true)
            String failureReason,

            @Schema(description = "환불 처리 일시")
            LocalDateTime refundedAt
    ) {}
}
