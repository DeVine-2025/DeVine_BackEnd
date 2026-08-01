package com.umc.devine.admin.member.dto;

import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class AdminMemberResDTO {

    @Builder
    public record MemberSummaryDTO(
            @Schema(description = "이름", example = "홍길동", nullable = true)
            String name,

            @Schema(description = "닉네임", example = "developer1")
            String nickname,

            @Schema(description = "이메일", example = "user@example.com", nullable = true)
            String email,

            @Schema(description = "계정 상태", example = "ACTIVE")
            MemberStatus status,

            @Schema(description = "가입일시")
            LocalDateTime createdAt
    ) {}

    @Builder
    public record PaymentDTO(
            @Schema(description = "결제 ID", example = "1")
            Long paymentId,

            @Schema(description = "주문명", example = "리포트 열람권 1개")
            String orderName,

            @Schema(description = "결제 금액", example = "5000")
            Long amount,

            @Schema(description = "결제일시")
            LocalDateTime createdAt
    ) {}

    @Builder
    public record PaymentSummaryDTO(
            @Schema(description = "총 결제 건수", example = "3")
            long totalCount,

            @Schema(description = "총 결제 금액", example = "15000")
            long totalAmount,

            @Schema(description = "최근 결제 내역 (최대 5건)")
            List<PaymentDTO> recentPayments
    ) {}

    @Builder
    public record LoginHistoryDTO(
            @Schema(description = "로그인 일시")
            LocalDateTime loginAt
    ) {}

    @Builder
    public record MemberDetailRes(
            @Schema(description = "이름", example = "홍길동", nullable = true)
            String name,

            @Schema(description = "닉네임", example = "developer1")
            String nickname,

            @Schema(description = "이메일", example = "user@example.com", nullable = true)
            String email,

            @Schema(description = "회원 유형", example = "DEVELOPER")
            MemberMainType mainType,

            @Schema(description = "계정 상태", example = "ACTIVE")
            MemberStatus status,

            @Schema(description = "강제탈퇴 예정일시 (PENDING_WITHDRAWAL 상태일 때만)", nullable = true)
            LocalDateTime scheduledWithdrawalAt,

            @Schema(description = "가입일시")
            LocalDateTime createdAt,

            @Schema(description = "결제 이력 요약")
            PaymentSummaryDTO paymentSummary,

            @Schema(description = "최근 로그인 이력 (최대 10건)")
            List<LoginHistoryDTO> loginHistory
    ) {}

    @Builder
    public record ChangeStatusRes(
            @Schema(description = "닉네임", example = "developer1")
            String nickname,

            @Schema(description = "변경된 계정 상태", example = "SUSPENDED")
            MemberStatus status,

            @Schema(description = "강제탈퇴 예정일시 (강제탈퇴 처리 시)", nullable = true)
            LocalDateTime scheduledWithdrawalAt
    ) {}
}
