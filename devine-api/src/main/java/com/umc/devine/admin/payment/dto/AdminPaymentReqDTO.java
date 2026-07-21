package com.umc.devine.admin.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminPaymentReqDTO {

    @Schema(description = "환불 요청")
    public record RefundDTO(
            @NotBlank(message = "환불 사유는 필수입니다.")
            @Size(max = 255, message = "환불 사유는 255자를 초과할 수 없습니다.")
            @Schema(description = "환불 사유", example = "고객 요청")
            String reason
    ) {}

    @Schema(description = "결제 내역 검색 조건")
    public record SearchDTO(
            @Schema(description = "유저 닉네임 (정확 일치)", example = "홍길동")
            String memberNickname,

            @Schema(description = "상품 ID (단건/묶음 구분)", example = "2")
            Long ticketProductId,

            @Schema(description = "결제일 시작 (포함)", example = "2026-07-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Schema(description = "결제일 종료 (포함)", example = "2026-07-21")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        public LocalDateTime paidFrom() {
            return startDate != null ? startDate.atStartOfDay() : null;
        }

        /** 종료일을 포함하기 위해 다음 날 00:00 미만으로 변환한다. */
        public LocalDateTime paidUntil() {
            return endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        }
    }
}
