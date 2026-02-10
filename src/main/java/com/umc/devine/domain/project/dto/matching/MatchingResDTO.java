package com.umc.devine.domain.project.dto.matching;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.techstack.enums.TechName;
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

            @Schema(description = "지원/제안 파트", example = "BACKEND")
            ProjectPart part,

            @Schema(description = "지원/제안 파트 이름", example = "백엔드")
            String partName,

            @Schema(description = "개발자 관심 도메인")
            List<CategoryInfo> categories,

            @Schema(description = "개발자 보유 기술 스택")
            List<TechName> techStacks,

            @Schema(description = "개발자 한줄소개")
            String body,

            @Schema(description = "매칭 타입", example = "APPLY")
            MatchingType matchingType,

            @Schema(description = "수락/거절 상태", example = "PENDING")
            MatchingDecision decision,

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

            @Schema(description = "프로젝트 썸네일 이미지 URL")
            String thumbnailUrl,

            @Schema(description = "프로젝트 분야", example = "WEB")
            ProjectField projectField,

            @Schema(description = "프로젝트 분야 이름", example = "웹")
            String projectFieldName,

            @Schema(description = "카테고리", example = "ECOMMERCE")
            CategoryGenre category,

            @Schema(description = "카테고리 이름", example = "이커머스")
            String categoryName,

            @Schema(description = "진행 장소", example = "서울 강남구")
            String location,

            @Schema(description = "진행 기간", example = "ONE_TO_THREE")
            DurationRange durationRange,

            @Schema(description = "진행 기간 이름", example = "1~3개월")
            String durationRangeName,

            @Schema(description = "진행 방식", example = "ONLINE")
            ProjectMode mode,

            @Schema(description = "진행 방식 이름", example = "온라인")
            String modeName,

            @Schema(description = "모집 파트별 인원 및 기술 스택 정보")
            List<PositionInfo> positions,

            @Schema(description = "제안 내용 (제안받은 경우에만)")
            String content,

            @Schema(description = "매칭 타입", example = "PROPOSE")
            MatchingType matchingType,

            @Schema(description = "수락/거절 상태", example = "PENDING")
            MatchingDecision decision,

            @Schema(description = "생성 일시")
            LocalDateTime createdAt
    ) {}

    @Builder
    public record CategoryInfo(
            @Schema(description = "카테고리", example = "FINTECH")
            CategoryGenre genre,

            @Schema(description = "카테고리 이름", example = "핀테크")
            String displayName
    ) {}

    @Builder
    public record PositionInfo(
            @Schema(description = "파트", example = "BACKEND")
            ProjectPart part,

            @Schema(description = "파트 이름", example = "백엔드")
            String partName,

            @Schema(description = "현재 인원", example = "1")
            Integer currentCount,

            @Schema(description = "모집 인원", example = "3")
            Integer requirementCount,

            @Schema(description = "해당 파트 기술 스택 목록")
            List<TechName> techStacks
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

    // 단순 매칭 상태 응답
    @Builder
    public record MatchingStatusRes(
            @Schema(description = "매칭 존재 여부", example = "true")
            boolean exists,

            @Schema(description = "매칭 ID (존재하지 않으면 null)", example = "1")
            Long matchingId,

            @Schema(description = "프로젝트 ID", example = "10")
            Long projectId,

            @Schema(description = "매칭 상태 (존재하지 않으면 null)", example = "PENDING")
            MatchingStatus status
    ) {
        public static MatchingStatusRes notFound(Long projectId) {
            return MatchingStatusRes.builder()
                    .exists(false)
                    .matchingId(null)
                    .projectId(projectId)
                    .status(null)
                    .build();
        }
    }
}
