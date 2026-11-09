package com.umc.devine.domain.member.dto;

import com.umc.devine.domain.member.enums.WithdrawalReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public class WithdrawalReqDTO {

    @Builder
    @Schema(description = "탈퇴 실행 요청(FUNC-MEM-003)")
    public record WithdrawReq(
            @Schema(description = "남은 생성권 환불 신청 여부. 환불 가능 생성권이 없으면 무시됩니다.", example = "true", defaultValue = "false")
            Boolean refundRequested,

            @Schema(description = "환불 연락 이메일(SCR-MEM-03에서 확인/수정한 값). 환불을 신청하면 필수", example = "user@example.com", nullable = true)
            @Email(message = "환불 연락 이메일 형식이 올바르지 않습니다.")
            @Size(max = 255, message = "환불 연락 이메일은 255자 이하여야 합니다.")
            String refundContactEmail,

            @Schema(description = "탈퇴 사유(선택, 단일 선택). 회원과 연결하지 않고 통계로만 남깁니다.", example = "OTHER", nullable = true)
            WithdrawalReason reason,

            @Schema(description = "안내를 확인했고 탈퇴합니다. true여야 합니다.", example = "true")
            @NotNull(message = "탈퇴 확인이 필요합니다.")
            @AssertTrue(message = "탈퇴 확인이 필요합니다.")
            Boolean confirmed
    ) {
        public WithdrawReq {
            if (refundRequested == null) refundRequested = false;
        }
    }
}
