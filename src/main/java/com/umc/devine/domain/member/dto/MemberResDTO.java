package com.umc.devine.domain.member.dto;

import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public class MemberResDTO {
    @Builder
    public record MemberDetailDTO(
            String name,
            String nickname,
            String address,
            Boolean disclosure,
            MemberMainType mainType,
            String image,
            String body,
            MemberStatus used,
            LocalDateTime createdAt
    ){}
}
