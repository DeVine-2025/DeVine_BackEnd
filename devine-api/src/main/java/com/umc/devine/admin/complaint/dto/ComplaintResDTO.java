package com.umc.devine.admin.complaint.dto;

import com.umc.devine.admin.complaint.enums.ComplaintAction;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class ComplaintResDTO {

    @Builder
    public record ComplaintSummaryDTO(
            @Schema(description = "신고 ID", example = "1")
            Long complaintId,

            @Schema(description = "신고 유형", example = "PROJECT")
            ComplaintTargetType targetType,

            @Schema(description = "신고자 닉네임", example = "complainant")
            String complainantNickname,

            @Schema(description = "피신고자 닉네임", example = "respondent")
            String respondentNickname,

            @Schema(description = "접수일시")
            LocalDateTime createdAt,

            @Schema(description = "처리 상태", example = "PENDING")
            ComplaintStatus status,

            @Schema(description = "접수 후 48시간 초과 여부", example = "false")
            boolean slaExceeded
    ) {}

    @Builder
    public record ComplaintDetailRes(
            @Schema(description = "신고 ID", example = "1")
            Long complaintId,

            @Schema(description = "신고 유형", example = "PROJECT")
            ComplaintTargetType targetType,

            @Schema(description = "신고 대상 ID (채팅 메시지ID/프로젝트ID). DEVELOPER 유형은 null", nullable = true)
            Long targetId,

            @Schema(description = "신고자 닉네임", example = "complainant")
            String complainantNickname,

            @Schema(description = "피신고자 닉네임", example = "respondent")
            String respondentNickname,

            @Schema(description = "신고 사유")
            String reason,

            @Schema(description = "처리 상태", example = "PENDING")
            ComplaintStatus status,

            @Schema(description = "세부 액션", nullable = true)
            ComplaintAction action,

            @Schema(description = "처리 사유", nullable = true)
            String resolutionReason,

            @Schema(description = "접수일시")
            LocalDateTime createdAt,

            @Schema(description = "처리시각", nullable = true)
            LocalDateTime resolvedAt,

            @Schema(description = "관련 콘텐츠 원문(채팅 로그/프로젝트 게시글). 삭제된 경우 안내 문구, DEVELOPER 유형은 null", nullable = true)
            String content,

            @Schema(description = "피신고자의 누적 신고 건수", example = "3")
            long respondentComplaintCount,

            @Schema(description = "피신고자의 과거 신고/제재 이력")
            List<ComplaintSummaryDTO> respondentHistory
    ) {}

    @Builder
    public record UpdateStatusRes(
            @Schema(description = "신고 ID", example = "1")
            Long complaintId,

            @Schema(description = "변경된 처리 상태", example = "COMPLETED")
            ComplaintStatus status,

            @Schema(description = "세부 액션", nullable = true)
            ComplaintAction action,

            @Schema(description = "처리 사유", nullable = true)
            String resolutionReason,

            @Schema(description = "처리시각", nullable = true)
            LocalDateTime resolvedAt,

            @Schema(description = "이미 처리완료된 건을 재변경한 경우 true (프론트 확인 다이얼로그 노출용)", example = "false")
            boolean reprocessWarning
    ) {}
}
