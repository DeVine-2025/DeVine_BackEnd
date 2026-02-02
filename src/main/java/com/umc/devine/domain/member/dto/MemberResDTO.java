package com.umc.devine.domain.member.dto;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.member.enums.ContactType;
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
            String imageUrl,
            String body,
            MemberStatus used,
            LocalDateTime createdAt
    ){}

    @Builder
    public record ContactDTO(
            ContactType type,
            String value,
            String link
    ){}

    @Builder
    public record MemberProfileDTO(
            MemberDetailDTO member,
            List<CategoryGenre> domains,
            List<ContactDTO> contacts
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

    @Builder
    public record DeveloperDTO(
            String nickname,
            String image,
            String body,
            List<String> techstacks
    ){}

    @Builder
    public record DeveloperListDTO(
            List<DeveloperDTO> developers
    ){}

    @Builder
    public record UserProfileListDTO(
            List<UserProfileDTO> developers
    ){}
}
