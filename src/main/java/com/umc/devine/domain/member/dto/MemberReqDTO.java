package com.umc.devine.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class MemberReqDTO {
    @Builder
    public record UpdateMemberDTO(
            @Schema(description = "닉네임", nullable = true, example = "devine") String nickname,
            @Schema(description = "프로필 이미지 url", nullable = true, example = "https://devine.com/image.jpg") String profileImageUrl,
            @Schema(description = "주소", nullable = true, example = "서울 구로구") String address,
            @Schema(description = "자기소개", nullable = true, example = "자기 소개 내용입니다.") String body
    ) {}
}
