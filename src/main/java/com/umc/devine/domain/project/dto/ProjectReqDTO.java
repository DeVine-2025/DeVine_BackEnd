package com.umc.devine.domain.project.dto;

import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.global.annotation.ValidPage;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class ProjectReqDTO {

        @Builder
        public record CreateProjectReq(
                        @Schema(description = "프로젝트 분야 (WEB, MOBILE, AI, GAME, DATA, BACKEND, FRONTEND)", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "프로젝트 분야는 필수입니다.") ProjectField projectField,

                        @Schema(description = "카테고리(도메인) ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1") @NotNull(message = "도메인은 필수입니다.") @Min(value = 1, message = "카테고리 ID는 1 이상이어야 합니다.") Long categoryId,

                        @Schema(description = "진행 방식 (ONLINE, OFFLINE, HYBRID)", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "진행 방식은 필수입니다.") ProjectMode mode,

                        @Schema(description = "진행 기간 (개월 단위, 예: 2)", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "진행 기간은 필수입니다.") @Min(value = 1, message = "진행 기간은 최소 한 달 이상이어야 합니다.") @Max(value = 13, message = "진행 기간은 최대 13개월 까지 가능합니다.") Integer durationMonths,

                        @Schema(description = "진행 장소 (ONLINE인 경우 '온라인', OFFLINE인 경우 구체적 장소)", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "진행 장소는 필수입니다.") @Size(max = 100, message = "진행 장소는 100자를 초과할 수 없습니다.") String location,

                        @Schema(description = "모집 마감일 (형식: YYYY-MM-DD)", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-25") @NotNull(message = "모집 마감일은 필수입니다.") LocalDate recruitmentDeadline,

                        @Schema(description = "프로젝트 시작 예정일 (형식: YYYY-MM-DD)", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-02-01") @NotNull(message = "프로젝트 시작일은 필수입니다.") LocalDate startDate,

                        @Schema(description = "모집 분야 목록 (포지션, 모집인원, 기술스택)", requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty(message = "모집 분야는 최소 1개 이상이어야 합니다.") @Size(max = 6, message = "모집 분야는 최대 6개까지 가능합니다.") @Valid List<RecruitmentDTO> recruitments,

                        @Schema(description = "프로젝트 제목", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "프로젝트 제목은 필수입니다.") @Size(max = 100, message = "프로젝트 제목은 100자를 초과할 수 없습니다.") String title,

                        @Schema(description = "프로젝트 내용", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "프로젝트 내용은 필수입니다.") @Size(max = 2000, message = "프로젝트 내용은 2000자를 초과할 수 없습니다.") String content,

                        @Schema(description = "프로젝트 이미지 URL 목록", requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Size(max = 10, message = "이미지는 최대 10개까지 등록 가능합니다.") List<@NotBlank(message = "이미지 URL은 빈 값일 수 없습니다.") @Pattern(regexp = "^https?://.*", message = "올바른 URL 형식이 아닙니다.") String> imageUrls) {
        }

        @Builder
        public record UpdateProjectReq(
                        @Schema(description = "프로젝트 분야 (WEB, MOBILE, AI, GAME, DATA, BACKEND, FRONTEND)", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "프로젝트 분야는 필수입니다.") ProjectField projectField,

                        @Schema(description = "카테고리(도메인) ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1") @NotNull(message = "도메인은 필수입니다.") @Min(value = 1, message = "카테고리 ID는 1 이상이어야 합니다.") Long categoryId,

                        @Schema(description = "진행 방식 (ONLINE, OFFLINE, HYBRID)", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "진행 방식은 필수입니다.") ProjectMode mode,

                        @Schema(description = "진행 기간 (개월 단위)", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "진행 기간은 필수입니다.") @Min(value = 1, message = "진행 기간은 최소 1개월 이상이어야 합니다.") @Max(value = 13, message = "진행 기간은 최대 13개월까지 가능합니다.") Integer durationMonths,

                        @Schema(description = "진행 장소", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "진행 장소는 필수입니다.") @Size(max = 100, message = "진행 장소는 100자를 초과할 수 없습니다.") String location,

                        @Schema(description = "모집 마감일", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "모집 마감일은 필수입니다.") LocalDate recruitmentDeadline,

                        @Schema(description = "프로젝트 시작 예정일", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "프로젝트 시작일은 필수입니다.") LocalDate startDate,

                        @Schema(description = "모집 분야 목록", requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty(message = "모집 분야는 최소 1개 이상이어야 합니다.") @Size(max = 6, message = "모집 분야는 최대 6개까지 가능합니다.") @Valid List<RecruitmentDTO> recruitments,

                        @Schema(description = "프로젝트 제목", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "프로젝트 제목은 필수입니다.") @Size(max = 100, message = "프로젝트 제목은 100자를 초과할 수 없습니다.") String title,

                        @Schema(description = "프로젝트 내용", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "프로젝트 내용은 필수입니다.") @Size(max = 2000, message = "프로젝트 내용은 2000자를 초과할 수 없습니다.") String content,

                        @Schema(description = "프로젝트 이미지 URL 목록", requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Size(max = 10, message = "이미지는 최대 10개까지 등록 가능합니다.") List<@NotBlank(message = "이미지 URL은 빈 값일 수 없습니다.") @Pattern(regexp = "^https?://.*", message = "올바른 URL 형식이 아닙니다.") String> imageUrls) {
        }

        @Builder
        public record RecruitmentDTO(
                        @Schema(description = "포지션 (BACKEND, FRONTEND, DESIGN, PM, IOS, ANDROID)", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "포지션은 필수입니다.") ProjectPart position,

                        @Schema(description = "모집 인원 (1~10명)", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "모집 인원은 필수입니다.") @Min(value = 1, message = "모집 인원은 최소 1명 이상이어야 합니다.") @Max(value = 10, message = "모집 인원은 최대 10명까지 가능합니다.") Integer count,

                        @Schema(description = "기술 스택 ID 목록", requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Size(max = 10, message = "기술 스택은 최대 10개까지 가능합니다.") List<@Min(value = 1, message = "기술 스택 ID는 1 이상이어야 합니다.") Long> techStackIds) {
        }

        // 프로젝트 검색 요청 DTO (프로젝트/개발자 보기 탭 하단)
        @Builder
        public record SearchProjectReq(
                        @ArraySchema(schema = @Schema(description = "프로젝트 유형 필터", implementation = ProjectField.class)) List<ProjectField> projectFields,

                        @ArraySchema(schema = @Schema(description = "카테고리(도메인) 필터", type = "integer", format = "int64", example = "1")) List<Long> categoryIds,

                        @ArraySchema(schema = @Schema(description = "포지션 필터", implementation = ProjectPart.class)) List<ProjectPart> positions,

                        @ArraySchema(schema = @Schema(description = "기술 스택 필터", type = "integer", format = "int64", example = "1")) List<Long> techStackIds,

                        @Schema(description = "진행 기간 범위 (UNDER_ONE: 1개월 이하, ONE_TO_THREE: 1~3개월, THREE_TO_SIX: 3~6개월, SIX_PLUS: 6개월 이상)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "ONE_TO_THREE") DurationRange durationRange,

                        @Schema(description = "페이지 번호 (1부터 시작)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "1", example = "1") @ValidPage Integer page,

                        @Schema(description = "페이지 크기", requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "4", example = "4") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.") @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.") Integer size) {
                public SearchProjectReq {
                        if (page == null)
                                page = 1;
                        if (size == null)
                                size = 4;
                }
        }

        // 통합 추천 프로젝트 요청 DTO
        @Builder
        public record RecommendProjectsReq(
                        @Schema(description = "응답 모드 (PREVIEW: limit개만 / PAGE: 페이징)", requiredMode = Schema.RequiredMode.REQUIRED, example = "PREVIEW") @NotNull(message = "mode는 필수입니다.") RecommendMode mode,

                        @Schema(description = "PREVIEW일 때 반환 개수 (4 또는 6). null이면 기본값 6", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "6") Integer limit,

                        @ArraySchema(schema = @Schema(description = "프로젝트 유형 필터")) List<ProjectField> projectFields,

                        @ArraySchema(schema = @Schema(description = "카테고리(도메인) 필터", type = "integer", format = "int64", example = "1")) List<Long> categoryIds,

                        @ArraySchema(schema = @Schema(description = "포지션 필터", implementation = ProjectPart.class)) List<ProjectPart> positions,

                        @ArraySchema(schema = @Schema(description = "기술 스택 필터", type = "integer", format = "int64", example = "1")) List<Long> techStackIds,

                        @Schema(description = "진행 기간 범위 (UNDER_ONE: 1개월 이하, ONE_TO_THREE: 1~3개월, THREE_TO_SIX: 3~6개월, SIX_PLUS: 6개월 이상)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "ONE_TO_THREE") DurationRange durationRange,

                        @Schema(description = "PAGE일 때 페이지 번호 (1부터 시작). null이면 1", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1") @ValidPage Integer page,

                        @Schema(description = "PAGE일 때 페이지 크기. null이면 4", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "4") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.") @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.") Integer size) {
                public RecommendProjectsReq {
                        if (page == null)
                                page = 1;
                        if (size == null)
                                size = 4;
                }
        }

        public enum RecommendMode {
                PREVIEW, PAGE
        }
}