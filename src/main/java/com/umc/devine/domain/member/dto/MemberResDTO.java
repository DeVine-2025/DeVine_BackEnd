package com.umc.devine.domain.member.dto;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MemberResDTO {

    @Builder
    @Schema(description = "회원가입 응답")
    public record SignupResultDTO(
            @Schema(description = "회원 ID", example = "1")
            Long memberId,

            @Schema(description = "닉네임", example = "devine")
            String nickname,

            @Schema(description = "역할", example = "DEVELOPER")
            MemberMainType mainType
    ) {}

    @Builder
    @Schema(description = "약관 목록 응답")
    public record TermsListDTO(
            @Schema(description = "약관 목록")
            List<TermsDTO> terms
    ) {}

    @Builder
    @Schema(description = "약관 정보")
    public record TermsDTO(
            @Schema(description = "약관 ID", example = "1")
            Long termsId,

            @Schema(description = "약관 제목", example = "서비스 이용약관")
            String title,

            @Schema(description = "약관 내용")
            String content,

            @Schema(description = "필수 동의 여부", example = "true")
            Boolean required
    ) {}

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
        LocalDate date,
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

    @Builder
    @Schema(description = "Git 레포지토리 정보")
    public record GitRepoDTO(
            @Schema(description = "Git 레포지토리 ID", example = "1")
            Long gitRepoId,

            @Schema(description = "레포지토리 이름", example = "my-project")
            String name,

            @Schema(description = "레포지토리 URL", example = "https://github.com/user/my-project")
            String gitUrl,

            @Schema(description = "레포지토리 설명", nullable = true)
            String description
    ) {}

    @Builder
    @Schema(description = "Git 레포지토리 목록 응답")
    public record GitRepoListDTO(
            @Schema(description = "레포지토리 목록")
            List<GitRepoDTO> repos
    ) {}
}
