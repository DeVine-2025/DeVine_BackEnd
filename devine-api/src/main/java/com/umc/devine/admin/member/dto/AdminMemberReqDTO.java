package com.umc.devine.admin.member.dto;

import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.global.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.data.domain.Pageable;

public class AdminMemberReqDTO {

    @Builder
    @Schema(description = "유저 검색/목록 조회 요청")
    public record SearchReq(
            @Schema(description = "검색어 (이름/닉네임/이메일). 미지정 시 전체 조회", nullable = true)
            @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
            String keyword,

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
    @Schema(description = "계정 상태 변경(정지/정지해제/강제탈퇴/강제탈퇴취소) 요청")
    public record ChangeStatusReq(
            @Schema(description = "처리 액션", example = "SUSPEND")
            @NotNull(message = "액션은 필수입니다.")
            MemberStatusAction action,

            @Schema(description = "처리 사유", example = "커뮤니티 이용규칙 위반")
            @Size(max = 500, message = "처리 사유는 500자 이하여야 합니다.")
            String reason,

            @Schema(description = "정지 시 안내 발송 여부", example = "true", defaultValue = "false")
            Boolean notifyRequested
    ) {
        public ChangeStatusReq {
            if (notifyRequested == null) notifyRequested = false;
        }
    }
}
