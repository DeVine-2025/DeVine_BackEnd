package com.umc.devine.domain.member.dto;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.techstack.enums.TechGenre;
import com.umc.devine.domain.techstack.enums.TechstackSource;
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
            String nickname,
            String address,
            Boolean disclosure,
            Boolean proposalAlarm,
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
    @Schema(description = "회원 목록 아이템 (목록 조회용)")
    public record MemberListItemDTO(
            MemberDetailDTO member,
            List<CategoryGenre> domains,
            List<TechstackItemDTO> techstacks
    ){}

    @Builder
    @Schema(description = "기술스택 아이템")
    public record TechstackItemDTO(
            Long techstackId,
            String name,
            TechGenre genre,
            TechstackSource source
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
    @Schema(description = "Git 레포지토리 정보")
    public record GitRepoDTO(
            @Schema(description = "Git 레포지토리 ID", example = "1")
            Long gitRepoId,

            @Schema(description = "레포지토리 이름", example = "my-project")
            String name,

            @Schema(description = "레포지토리 URL", example = "https://github.com/user/my-project")
            String gitUrl,

            @Schema(description = "레포지토리 설명", nullable = true)
            String description,

            @Schema(description = "리포트 생성 여부", example = "false")
            Boolean hasReport
    ) {}

    @Builder
    @Schema(description = "Git 레포지토리 목록 응답")
    public record GitRepoListDTO(
            @Schema(description = "레포지토리 목록")
            List<GitRepoDTO> repos
    ) {}

    @Builder
    @Schema(description = "추천 개발자 아이템")
    public record RecommendedDeveloperDTO(
            @Schema(description = "회원 정보")
            MemberDetailDTO member,

            @Schema(description = "관심 도메인 목록")
            List<CategoryGenre> domains,

            @Schema(description = "기술스택 목록")
            List<TechstackItemDTO> techstacks,

            @Schema(description = "총 추천 점수 (100점 만점)", example = "83.0")
            Double totalScore,

            @Schema(description = "리포트 유사도 (100점 만점)", example = "80.0")
            Double similarityScorePercent,

            @Schema(description = "기술 스택 일치도 (100점 만점)", example = "75.0")
            Double techstackScorePercent,

            @Schema(description = "도메인 일치 여부", example = "true")
            Boolean domainMatch,

            @Schema(description = "프로젝트 요구사항과 일치하는 기술스택 목록", example = "[\"JAVA\", \"SPRING\"]")
            List<String> matchedTechstacks
    ){}

    @Builder
    @Schema(description = "추천 개발자 목록 응답")
    public record RecommendedDevelopersDTO(
            @Schema(description = "기준 프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "추천 개발자 목록 (상위 10명)")
            List<RecommendedDeveloperDTO> developers,

            @Schema(description = "반환된 개발자 수", example = "10")
            Integer count
    ){}
}
