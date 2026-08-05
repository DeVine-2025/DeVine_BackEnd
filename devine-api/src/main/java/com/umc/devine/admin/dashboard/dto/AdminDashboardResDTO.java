package com.umc.devine.admin.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class AdminDashboardResDTO {

    @Schema(description = "관리자 홈 대시보드 지표. 조회에 실패한 지표는 null로 내려간다.")
    public record DashboardDTO(
            @Schema(description = "처리 대기(PENDING) 상태인 신고 건수", example = "12")
            Long pendingComplaintCount,

            @Schema(description = "전체 쿠폰 사용률(%). 발급된 쿠폰이 없으면 0.0", example = "34.5")
            Double couponUsageRate,

            @Schema(description = "오늘 결제 완료된 건수", example = "7")
            Long todayPaymentCount
    ) {}
}
