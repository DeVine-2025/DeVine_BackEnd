package com.umc.devine.admin.complaint.dto;

import com.umc.devine.admin.complaint.enums.ComplaintAction;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.global.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public class ComplaintReqDTO {

    @Builder
    @Schema(description = "신고 목록 조회 요청")
    public record SearchReq(
            @Schema(description = "신고 유형 필터 (CHAT, PROJECT, DEVELOPER). 미지정 시 전체 조회", nullable = true)
            ComplaintTargetType targetType,

            @Schema(description = "처리 상태 필터 (PENDING, IN_REVIEW, COMPLETED). 미지정 시 전체 조회", nullable = true)
            ComplaintStatus status,

            @Schema(description = "조회 시작일", nullable = true)
            LocalDate fromDate,

            @Schema(description = "조회 종료일", nullable = true)
            LocalDate toDate,

            @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            Integer page,

            @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            Integer size
    ) {
        public SearchReq {
            if (page == null) page = 1;
            if (size == null) size = 10;
        }

        public Pageable toPageable() {
            return PageRequest.of(page, size).toPageable();
        }
    }

    @Builder
    @Schema(description = "신고 처리 상태 변경 요청")
    public record UpdateStatusReq(
            @Schema(description = "변경할 상태", example = "IN_REVIEW")
            @NotNull(message = "상태는 필수입니다.")
            ComplaintStatus status,

            @Schema(description = "세부 액션 (WARNING, DELETE, SUSPEND, DISMISS). 처리완료 시 필수", nullable = true)
            ComplaintAction action,

            @Schema(description = "처리 사유. 처리완료 시 필수", nullable = true)
            String reason
    ) {}
}
