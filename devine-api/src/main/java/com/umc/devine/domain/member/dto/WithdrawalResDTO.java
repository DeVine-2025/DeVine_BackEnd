package com.umc.devine.domain.member.dto;

import com.umc.devine.domain.project.enums.mapping.MatchingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class WithdrawalResDTO {

    @Builder
    @Schema(description = "탈퇴 미리보기 응답(FUNC-MEM-001). 탈퇴 화면을 띄울 때마다 다시 조회합니다.")
    public record PreviewDTO(
            @Schema(description = "수락/거절 전인 지원/제안 목록. 1건 이상이면 정리해야 탈퇴할 수 있습니다.")
            List<PendingMatchingDTO> pendingMatchings,

            @Schema(description = "대기 중인 지원/제안 수", example = "2")
            int pendingCount,

            @Schema(description = "지워질 리포트 수(완료된 리포트, 생성권 1개당 1개)", example = "3")
            long reportCount,

            @Schema(description = "생성 중인 리포트가 있는지. 있으면 탈퇴 시 결과가 사라지고 생성권은 돌아오지 않습니다.", example = "false")
            boolean generatingReport,

            @Schema(description = "남은 생성권 전체", example = "3")
            int remainingCredits,

            @Schema(description = "환불 가능한 생성권 수", example = "2")
            int refundableCredits,

            @Schema(description = "환불 불가 생성권 수(유효기간 만료 또는 무상 지급)", example = "1")
            int nonRefundableCredits,

            @Schema(description = "환불 예정 금액(원)", example = "6600")
            long refundAmount,

            @Schema(description = "환불 연락 이메일 기본값(가입 이메일). 없으면 null", example = "user@example.com", nullable = true)
            String refundContactEmail
    ) {}

    @Builder
    @Schema(description = "대기 중인 지원/제안")
    public record PendingMatchingDTO(
            @Schema(description = "매칭 ID", example = "10")
            Long matchingId,

            @Schema(description = "프로젝트 이름", example = "DeVine")
            String projectName,

            @Schema(description = "상대방 닉네임", example = "devine_pm")
            String counterpartNickname,

            @Schema(description = "지원(APPLY) / 제안(PROPOSE)", example = "APPLY")
            MatchingType matchingType,

            @Schema(description = "내가 보낸 건(SENT) / 받은 건(RECEIVED)", example = "SENT")
            Direction direction
    ) {}

    public enum Direction {
        SENT, RECEIVED
    }

    @Builder
    @Schema(description = "탈퇴 실행 응답(FUNC-MEM-003)")
    public record WithdrawResultDTO(
            @Schema(description = "탈퇴 완료 여부", example = "true")
            boolean withdrawn,

            @Schema(description = "환불 신청이 접수됐는지. 환불 가능 생성권이 없었으면 false", example = "true")
            boolean refundRequested
    ) {}

    @Builder
    @Schema(description = "탈퇴 409 응답의 result: 남은 대기 건 수. 화면은 SCR-MEM-01로 돌아갑니다.")
    public record PendingMatchingsRemainDTO(
            @Schema(description = "대기 중인 지원/제안 수", example = "2")
            int pendingCount
    ) {}

    @Builder
    @Schema(description = "대기 지원/제안 일괄 취소 응답(FUNC-MEM-002)")
    public record CancelPendingResultDTO(
            @Schema(description = "취소한 건수. 대기 건이 없으면 0", example = "2")
            int canceledCount
    ) {}
}
