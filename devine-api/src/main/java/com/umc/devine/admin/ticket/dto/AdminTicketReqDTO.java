package com.umc.devine.admin.ticket.dto;

import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import com.umc.devine.global.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import org.springframework.data.domain.Pageable;

public class AdminTicketReqDTO {

    @Builder
    @Schema(description = "환불 신청 목록 조회 요청")
    public record RefundRequestSearchReq(
            @Schema(description = "처리 상태 필터. 미지정 시 전체 조회", example = "REQUESTED", nullable = true)
            CreditRefundStatus status,

            @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            Integer page,

            @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            Integer size
    ) {
        public RefundRequestSearchReq {
            if (page == null) page = 1;
            if (size == null) size = 10;
        }

        public Pageable toPageable() {
            return PageRequest.of(page, size).toPageable();
        }
    }
}
