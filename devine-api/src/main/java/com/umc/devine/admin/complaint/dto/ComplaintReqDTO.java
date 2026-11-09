package com.umc.devine.admin.complaint.dto;

import com.umc.devine.domain.complaint.enums.ComplaintAction;
import com.umc.devine.domain.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.global.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

            @Schema(description = "세부 액션 (WARNING, DELETE, SUSPEND, DISMISS, WITHDRAWN). 처리완료 시 필수", nullable = true)
            ComplaintAction action,

            @Schema(description = "내부 처리 사유. 처리완료 시 필수. 알림·이메일에는 쓰이지 않는다", nullable = true)
            String reason,

            @Schema(description = "통보용 사유 요약. 처리완료 시 필수. 신고자·피신고자 알림에 그대로 들어가므로 "
                    + "신고자를 짐작할 내용과 제재 내용은 쓰지 않는다", nullable = true)
            @Size(max = 500, message = "통보용 사유 요약은 500자 이하여야 합니다.")
            String notifySummary
    ) {}
}
