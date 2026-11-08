package com.umc.devine.domain.complaint.dto;

import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class ComplaintResDTO {

    @Builder
    @Schema(description = "신고 접수 결과")
    public record CreateRes(
            @Schema(description = "신고 번호. 이의제기는 이 번호로 접수한다.", example = "12")
            Long complaintId,

            @Schema(description = "신고 대상 종류", example = "PROJECT")
            ComplaintTargetType targetType,

            @Schema(description = "화면에 보여줄 대상 이름 (프로젝트명 / 닉네임 / \"채팅 메시지\")", example = "DeVine 팀 모집")
            String targetName,

            @Schema(description = "차단 제안 대상 회원의 닉네임 (프로젝트는 PM, 그 외는 상대)", example = "devine_user")
            String blockTargetNickname,

            @Schema(description = "그 회원을 이미 차단했는지 여부. 차단 기능 추가 전까지는 항상 false다.", example = "false")
            boolean blockTargetAlreadyBlocked
    ) {}
}
