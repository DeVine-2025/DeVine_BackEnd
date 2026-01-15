package com.umc.devine.domain.member.dto;

import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

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

    @Builder
    public record UserProfileDTO(
        String nickname,
        String address,
        String image,
        String body,
        List<String> techstacks,
        List<String> techGenres
    ){}

    @Builder
    public record NicknameDuplicateDTO(
        String nickname,
        Boolean isDuplicate
    ){}

    @Builder
    public record ContributionDTO(
        String date,
        Integer count
    ){}
    @Builder
    public record ContributionListDTO(
            List<ContributionDTO>  contributionList
    ){}
}
