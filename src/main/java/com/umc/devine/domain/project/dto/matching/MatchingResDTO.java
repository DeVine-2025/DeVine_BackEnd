package com.umc.devine.domain.project.dto.matching;

import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.global.dto.PagedResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class MatchingResDTO {

    @Builder
    public record ProposeResDTO (
        Long matchingId,
        Long projectId,
        String projectName,
        Long memberId,
        String memberNickname,
        MatchingStatus status,
        MatchingType matchingType,
        LocalDateTime createdAt
    ) {}

    // PM용: 프로젝트에 지원한/제안한 개발자 정보
    @Builder
    public record DeveloperMatchingInfo(
            @Schema(description = "매칭 ID", example = "1")
            Long matchingId,

            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "프로젝트 이름", example = "AI 협업 플랫폼")
            String projectName,

            @Schema(description = "개발자 ID", example = "10")
            Long developerId,

            @Schema(description = "개발자 닉네임", example = "devkim")
            String developerNickname,

            @Schema(description = "개발자 프로필 이미지 URL")
            String developerImageUrl,

            @Schema(description = "매칭 상태", example = "PENDING")
            MatchingStatus status,

            @Schema(description = "매칭 타입", example = "APPLY")
            MatchingType matchingType,

            @Schema(description = "생성 일시")
            LocalDateTime createdAt
    ) {}

    // 개발자용: 제안받은/지원한 프로젝트 정보
    @Builder
    public record ProjectMatchingInfo(
            @Schema(description = "매칭 ID", example = "1")
            Long matchingId,

            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "프로젝트 이름", example = "AI 협업 플랫폼")
            String projectName,

            @Schema(description = "PM ID", example = "5")
            Long pmId,

            @Schema(description = "PM 닉네임", example = "pmlee")
            String pmNickname,

            @Schema(description = "PM 프로필 이미지 URL")
            String pmImageUrl,

            @Schema(description = "매칭 상태", example = "PENDING")
            MatchingStatus status,

            @Schema(description = "매칭 타입", example = "PROPOSE")
            MatchingType matchingType,

            @Schema(description = "생성 일시")
            LocalDateTime createdAt
    ) {}

    // PM용: 개발자 목록 응답 (제안한/지원받은)
    @Builder
    public record DevelopersRes(
            @Schema(description = "개발자 목록")
            PagedResponse<DeveloperMatchingInfo> developers
    ) {}

    // 개발자용: 프로젝트 목록 응답 (제안받은/지원한)
    @Builder
    public record ProjectsRes(
            @Schema(description = "프로젝트 목록")
            PagedResponse<ProjectMatchingInfo> projects
    ) {}
}
