package com.umc.devine.domain.complaint.dto;

import com.umc.devine.domain.complaint.enums.ComplaintReasonCode;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public class ComplaintReqDTO {

    @Builder
    @Schema(description = "신고 접수 요청")
    public record CreateReq(
            @Schema(description = "신고 대상 종류 (PROJECT: 프로젝트, DEVELOPER: 회원, CHAT: 채팅 메시지)", example = "PROJECT")
            @NotNull(message = "신고 대상 종류는 필수입니다.")
            ComplaintTargetType targetType,

            @Schema(description = "PROJECT면 프로젝트 ID, CHAT이면 채팅 메시지 ID. DEVELOPER면 비워 둡니다.", nullable = true)
            Long targetId,

            @Schema(description = "DEVELOPER 신고 대상 회원의 닉네임. 다른 유형에서는 무시됩니다.", nullable = true)
            String targetNickname,

            @Schema(description = "신고 사유 (ABUSE: 욕설·비방, SPAM: 스팸·광고, FRAUD: 사기·금전 요구, "
                    + "SEXUAL_HARASSMENT: 성희롱, FALSE_INFORMATION: 허위 정보, PRIVACY_VIOLATION: 개인정보 침해, ETC: 기타)",
                    example = "ABUSE")
            @NotNull(message = "신고 사유는 필수입니다.")
            ComplaintReasonCode reasonCode,

            @Schema(description = "상세 내용. 사유가 ETC면 필수이고, 그 외에는 선택입니다.", nullable = true)
            @Size(max = 500, message = "상세 내용은 500자 이하여야 합니다.")
            String detail
    ) {}
}
