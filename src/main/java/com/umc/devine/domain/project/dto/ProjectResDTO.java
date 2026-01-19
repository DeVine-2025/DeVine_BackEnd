package com.umc.devine.domain.project.dto;

import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.global.dto.PagedResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class ProjectResDTO {

    @Builder
    public record CreateProjectRes(
            @Schema(description = "생성된 프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "프로젝트 분야", example = "WEB")
            ProjectField projectField,

            @Schema(description = "프로젝트 분야 이름", example = "웹")
            String projectFieldName,

            @Schema(description = "카테고리 ID", example = "1")
            Long categoryId,

            @Schema(description = "카테고리 이름", example = "ECOMMERCE")
            String categoryName,

            @Schema(description = "진행 방식", example = "HYBRID")
            ProjectMode mode,

            @Schema(description = "진행 방식 이름", example = "온라인")
            String modeName,

            @Schema(description = "진행 기간 (개월 단위)", example = "2")
            Integer durationMonths,

            @Schema(description = "진행 장소", example = "서울 강남구")
            String location,

            @Schema(description = "모집 마감일", example = "2026-01-25")
            LocalDate recruitmentDeadline,

            @Schema(description = "프로젝트 시작 예정일", example = "2026-02-01")
            LocalDate startDate,

            @Schema(description = "프로젝트 종료 예정일", example = "2026-04-30")
            LocalDate eta,

            @Schema(description = "프로젝트 제목", example = "AI 기반 협업 플랫폼")
            String title,

            @Schema(description = "프로젝트 내용", example = "GitHub 분석 기반 개발자 매칭 서비스를 개발합니다.")
            String content,

            @Schema(description = "프로젝트 상태", example = "RECRUITING")
            ProjectStatus status,

            @Schema(description = "프로젝트 생성자 ID", example = "1")
            Long creatorId,

            @Schema(description = "프로젝트 생성자 이름", example = "김개발")
            String creatorName,

            @Schema(description = "모집 분야 목록")
            List<RecruitmentInfo> recruitments,

            @Schema(description = "프로젝트 이미지 URL 목록")
            List<String> imageUrls
    ) {}

    @Builder
    public record UpdateProjectRes(
            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "프로젝트 분야", example = "WEB")
            ProjectField projectField,

            @Schema(description = "프로젝트 분야 이름", example = "웹")
            String projectFieldName,

            @Schema(description = "카테고리 ID", example = "1")
            Long categoryId,

            @Schema(description = "카테고리 이름", example = "ECOMMERCE")
            String categoryName,

            @Schema(description = "진행 방식", example = "HYBRID")
            ProjectMode mode,

            @Schema(description = "진행 방식 이름", example = "온라인")
            String modeName,

            @Schema(description = "진행 기간 (개월 단위)", example = "2")
            Integer durationMonths,

            @Schema(description = "진행 장소", example = "서울 강남구")
            String location,

            @Schema(description = "모집 마감일", example = "2026-01-25")
            LocalDate recruitmentDeadline,

            @Schema(description = "프로젝트 시작 예정일", example = "2026-02-01")
            LocalDate startDate,

            @Schema(description = "프로젝트 종료 예정일", example = "2026-04-30")
            LocalDate eta,

            @Schema(description = "프로젝트 제목", example = "수정된 AI 협업 플랫폼")
            String title,

            @Schema(description = "프로젝트 내용", example = "수정된 내용입니다.")
            String content,

            @Schema(description = "프로젝트 상태", example = "RECRUITING")
            ProjectStatus status,

            @Schema(description = "프로젝트 생성자 ID", example = "1")
            Long creatorId,

            @Schema(description = "프로젝트 생성자 이름", example = "김개발")
            String creatorName,

            @Schema(description = "모집 분야 목록")
            List<RecruitmentInfo> recruitments,

            @Schema(description = "프로젝트 이미지 URL 목록")
            List<String> imageUrls
    ) {}

    @Builder
    public record RecruitmentInfo(
            @Schema(description = "포지션", example = "BACKEND")
            ProjectPart position,

            @Schema(description = "모집 인원", example = "2")
            Integer count,

            @Schema(description = "현재 모집된 인원", example = "0")
            Integer currentCount,

            @Schema(description = "기술 스택 목록 (ID와 이름)")
            List<TechStackInfo> techStacks
    ) {}

    @Builder
    public record TechStackInfo(
            @Schema(description = "기술 스택 ID", example = "1")
            Long techStackId,

            @Schema(description = "기술 스택 이름", example = "JAVA")
            String techStackName
    ) {}

    @Builder
    public record ProjectSummary(
            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "프로젝트 제목", example = "AI 기반 추천 시스템")
            String title,

            @Schema(description = "프로젝트 분야")
            ProjectField projectField,

            @Schema(description = "프로젝트 분야 이름", example = "웹")
            String projectFieldName,

            @Schema(description = "카테고리 이름", example = "ECOMMERCE")
            String categoryName,

            @Schema(description = "진행 방식")
            ProjectMode mode,

            @Schema(description = "진행 방식 이름", example = "온/오프라인")
            String modeName,

            @Schema(description = "진행 기간 (개월)", example = "3")
            Integer durationMonths,

            @Schema(description = "진행 장소", example = "서울 강남구")
            String location,

            @Schema(description = "모집 마감일", example = "2026-01-25")
            LocalDate recruitmentDeadline,

            @Schema(description = "프로젝트 상태")
            ProjectStatus status,

            @Schema(description = "썸네일 이미지 URL")
            String thumbnailUrl,

            @Schema(description = "모집 포지션 목록 (간략 정보)")
            List<PositionSummary> positions,

            @Schema(description = "생성자 이름", example = "김개발")
            String creatorName
    ) {}

    @Builder
    public record RecommendedProjectSummary(
            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "프로젝트 제목", example = "AI 기반 추천 시스템")
            String title,

            @Schema(description = "프로젝트 분야")
            ProjectField projectField,

            @Schema(description = "프로젝트 분야 이름", example = "웹")
            String projectFieldName,

            @Schema(description = "카테고리 이름", example = "ECOMMERCE")
            String categoryName,

            @Schema(description = "진행 방식")
            ProjectMode mode,

            @Schema(description = "진행 방식 이름", example = "온/오프라인")
            String modeName,

            @Schema(description = "진행 기간 (개월)", example = "3")
            Integer durationMonths,

            @Schema(description = "진행 장소", example = "서울 강남구")
            String location,

            @Schema(description = "모집 마감일", example = "2026-01-25")
            LocalDate recruitmentDeadline,

            @Schema(description = "프로젝트 상태")
            ProjectStatus status,

            @Schema(description = "썸네일 이미지 URL")
            String thumbnailUrl,

            @Schema(description = "모집 포지션 목록 (간략 정보)")
            List<PositionSummary> positions,

            @Schema(description = "생성자 이름", example = "김개발")
            String creatorName,

            @Schema(description = "기술 스택 적합도 점수 (0~5)", example = "4")
            Integer techScore,

            @Schema(description = "도메인 적합도 점수 (0~5)", example = "4")
            Integer domainScore,

            @Schema(description = "기술 스택 다양성 점수 (0~5)", example = "3")
            Integer techStackCountScore,

            @Schema(description = "총 추천 점수 (0~100)", example = "73")
            Integer totalScore
    ) {}

    @Builder
    public record PositionSummary(
            @Schema(description = "포지션", example = "BACKEND")
            ProjectPart position,

            @Schema(description = "포지션 이름", example = "백엔드")
            String positionName,

            @Schema(description = "모집 인원", example = "2")
            Integer count,

            @Schema(description = "현재 모집된 인원", example = "0")
            Integer currentCount,

            @Schema(description = "기술 스택 목록 (ID와 이름)")
            List<TechStackInfo> techStacks
    ) {}

    @Builder
    public record WeeklyBestProjectsRes(
            @Schema(description = "이번 주 주목 프로젝트 목록 (최대 4개)")
            List<ProjectSummary> projects
    ) {}

    @Builder
    public record SearchProjectsRes(
            @Schema(description = "프로젝트 검색 결과 (페이징)")
            PagedResponse<ProjectSummary> projects
    ) {}

    @Builder
    public record RecommendedProjectsRes(
            @Schema(description = "추천 프로젝트 결과")
            PagedResponse<RecommendedProjectSummary> projects
    ) {}
}