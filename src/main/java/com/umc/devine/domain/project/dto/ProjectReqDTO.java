package com.umc.devine.domain.project.dto;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.global.validation.annotation.ValidPage;
import com.umc.devine.global.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public class ProjectReqDTO {

        @Builder
        public record CreateProjectReq(
                        @Schema(description = "프로젝트 분야 (WEB, MOBILE, GAME, BLOCKCHAIN, ETC)", requiredMode = Schema.RequiredMode.REQUIRED, example = "WEB") @NotNull(message = "프로젝트 분야는 필수입니다.") ProjectField projectField,

                        @Schema(description = "도메인 (HEALTHCARE, FINTECH, ECOMMERCE, EDUCATION, SOCIAL, ENTERTAINMENT, AI_DATA, ETC)", requiredMode = Schema.RequiredMode.REQUIRED, example = "ECOMMERCE") @NotNull(message = "도메인은 필수입니다.") CategoryGenre category,

                        @Schema(description = "진행 방식 (ONLINE, OFFLINE, HYBRID)", requiredMode = Schema.RequiredMode.REQUIRED, example = "OFFLINE") @NotNull(message = "진행 방식은 필수입니다.") ProjectMode mode,

                        @Schema(description = "진행 기간 (UNDER_ONE: 1개월 이하, ONE_TO_THREE: 1~3개월, THREE_TO_SIX: 3~6개월, SIX_PLUS: 6개월 이상)", requiredMode = Schema.RequiredMode.REQUIRED, example = "ONE_TO_THREE") @NotNull(message = "진행 기간은 필수입니다.") DurationRange durationRange,

                        @Schema(description = "진행 장소 (ONLINE인 경우 '온라인', OFFLINE인 경우 구체적 장소)", requiredMode = Schema.RequiredMode.REQUIRED, example = "서울 강남구") @NotBlank(message = "진행 장소는 필수입니다.") @Size(max = 100, message = "진행 장소는 100자를 초과할 수 없습니다.") String location,

                        @Schema(description = "모집 마감일 (형식: YYYY-MM-DD)", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-03-05") @NotNull(message = "모집 마감일은 필수입니다.") LocalDate recruitmentDeadline,

                        @Schema(description = "모집 분야 목록 (포지션, 모집인원, 기술스택)", requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty(message = "모집 분야는 최소 1개 이상이어야 합니다.") @Size(max = 6, message = "모집 분야는 최대 6개까지 가능합니다.") @Valid List<RecruitmentDTO> recruitments,

                        @Schema(description = "프로젝트 제목", requiredMode = Schema.RequiredMode.REQUIRED, example = "AI 기반 협업 플랫폼") @NotBlank(message = "프로젝트 제목은 필수입니다.") @Size(max = 100, message = "프로젝트 제목은 100자를 초과할 수 없습니다.") String title,

                        @Schema(description = "프로젝트 내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "GitHub 분석 기반 개발자 매칭 서비스를 개발합니다.") @NotBlank(message = "프로젝트 내용은 필수입니다.") String content,

                        @Schema(description = "프로젝트 대표사진 이미지 ID 목록 (최대 3개)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "[1, 2]") @Size(max = 3, message = "대표사진은 최대 3개까지 등록 가능합니다.") List<@Min(value = 1, message = "이미지 ID는 1 이상이어야 합니다.") Long> imageIds) {
        }

        @Builder
        public record UpdateProjectReq(
                        @Schema(description = "프로젝트 분야 (WEB, MOBILE, GAME, BLOCKCHAIN, ETC)", requiredMode = Schema.RequiredMode.REQUIRED, example = "WEB") @NotNull(message = "프로젝트 분야는 필수입니다.") ProjectField projectField,

                        @Schema(description = "도메인 (HEALTHCARE, FINTECH, ECOMMERCE, EDUCATION, SOCIAL, ENTERTAINMENT, AI_DATA, ETC)", requiredMode = Schema.RequiredMode.REQUIRED, example = "ECOMMERCE") @NotNull(message = "도메인은 필수입니다.") CategoryGenre category,

                        @Schema(description = "진행 방식 (ONLINE, OFFLINE, HYBRID)", requiredMode = Schema.RequiredMode.REQUIRED, example = "OFFLINE") @NotNull(message = "진행 방식은 필수입니다.") ProjectMode mode,

                        @Schema(description = "진행 기간 (UNDER_ONE: 1개월 이하, ONE_TO_THREE: 1~3개월, THREE_TO_SIX: 3~6개월, SIX_PLUS: 6개월 이상)", requiredMode = Schema.RequiredMode.REQUIRED, example = "ONE_TO_THREE") @NotNull(message = "진행 기간은 필수입니다.") DurationRange durationRange,

                        @Schema(description = "진행 장소", requiredMode = Schema.RequiredMode.REQUIRED, example = "서울 강남구") @NotBlank(message = "진행 장소는 필수입니다.") @Size(max = 100, message = "진행 장소는 100자를 초과할 수 없습니다.") String location,

                        @Schema(description = "모집 마감일", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "모집 마감일은 필수입니다.") LocalDate recruitmentDeadline,

                        @Schema(description = "모집 분야 목록", requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty(message = "모집 분야는 최소 1개 이상이어야 합니다.") @Size(max = 6, message = "모집 분야는 최대 6개까지 가능합니다.") @Valid List<RecruitmentDTO> recruitments,

                        @Schema(description = "프로젝트 제목", requiredMode = Schema.RequiredMode.REQUIRED, example = "AI 기반 협업 플랫폼") @NotBlank(message = "프로젝트 제목은 필수입니다.") @Size(max = 100, message = "프로젝트 제목은 100자를 초과할 수 없습니다.") String title,

                        @Schema(description = "프로젝트 내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "GitHub 분석 기반 개발자 매칭 서비스를 개발합니다.") @NotBlank(message = "프로젝트 내용은 필수입니다.") String content,

                        @Schema(description = "프로젝트 대표사진 이미지 ID 목록 (최대 3개)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "[1, 2]") @Size(max = 3, message = "대표사진은 최대 3개까지 등록 가능합니다.") List<@Min(value = 1, message = "이미지 ID는 1 이상이어야 합니다.") Long> imageIds) {
        }

        @Builder
        public record RecruitmentDTO(
                        @Schema(description = "포지션 (FRONTEND, BACKEND, INFRA)", requiredMode = Schema.RequiredMode.REQUIRED, example = "BACKEND") @NotNull(message = "포지션은 필수입니다.") ProjectPart position,

                        @Schema(description = "모집 인원 (1~8명)", requiredMode = Schema.RequiredMode.REQUIRED, example = "2") @NotNull(message = "모집 인원은 필수입니다.") @Min(value = 1, message = "모집 인원은 최소 1명 이상이어야 합니다.") @Max(value = 8, message = "모집 인원은 최대 8명까지 가능합니다.") Integer count,

                        @Schema(description = "기술 스택 목록 (JAVA, JAVASCRIPT, REACT, SPRINGBOOT 등)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "[\"JAVA\"]") @Size(max = 10, message = "기술 스택은 최대 10개까지 가능합니다.") List<TechName> techStacks) {
        }

        // 프로젝트 검색 요청 DTO (프로젝트/개발자 보기 탭 하단)
        @Builder
        public record SearchProjectReq(
                        @Schema(description = "프로젝트 유형 목록 (ALL, WEB, MOBILE, GAME, BLOCKCHAIN, ETC), 복수 선택 가능", nullable = true, example = "[\"WEB\", \"MOBILE\"]") List<ProjectField> projectFields,

                        @Schema(description = "도메인 목록 (ALL, HEALTHCARE, FINTECH, ECOMMERCE, EDUCATION, SOCIAL, ENTERTAINMENT, AI_DATA, ETC), 복수 선택 가능", nullable = true, example = "[\"ECOMMERCE\", \"FINTECH\"]") List<CategoryGenre> categories,

                        @Schema(description = "포지션 목록 (ALL, FRONTEND, BACKEND, INFRA), 복수 선택 가능", nullable = true, example = "[\"BACKEND\", \"FRONTEND\"]") List<ProjectPart> positions,

                        @Schema(description = "기술스택 목록 (JAVA, JAVASCRIPT, REACT, SPRINGBOOT 등), 복수 선택 가능", nullable = true, example = "[\"JAVA\", \"SPRINGBOOT\"]") List<TechName> techstackNames,

                        @Schema(description = "예상 기간 목록 (UNDER_ONE, ONE_TO_THREE, THREE_TO_SIX, SIX_PLUS), 복수 선택 가능", nullable = true, example = "[\"ONE_TO_THREE\", \"THREE_TO_SIX\"]") List<DurationRange> durationRanges,

                        @Schema(description = "페이지 번호 (1부터 시작)", nullable = true, defaultValue = "1", example = "1") @ValidPage Integer page,

                        @Schema(description = "페이지 크기", nullable = true, defaultValue = "10", example = "10") Integer size) {
                public SearchProjectReq {
                        if (page == null)
                                page = 1;
                        if (size == null)
                                size = 10;
                }

                public Pageable toPageable() {
                        return PageRequest.of(page, size).toPageable();
                }
        }

        // 추천 프로젝트 미리보기 요청 DTO (메인 하단 / 프로젝트·개발자 보기 탭 상단)
        @Builder
        public record RecommendProjectsPreviewReq(
                        @Schema(description = "반환할 프로젝트 개수 (4 또는 6만 허용, 미입력 시 기본값 6)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "6", allowableValues = {"4", "6"}) @Min(value = 4) @Max(value = 6) Integer limit) {
                public RecommendProjectsPreviewReq {
                        if (limit == null) {
                                limit = 6;
                        }
                }
        }

        // 추천 프로젝트 페이지 요청 DTO (추천 프로젝트 탭용)
        @Builder
        public record RecommendProjectsPageReq(
                        @Schema(description = "프로젝트 유형 목록 (ALL, WEB, MOBILE, GAME, BLOCKCHAIN, ETC), 복수 선택 가능", nullable = true, example = "[\"WEB\", \"MOBILE\"]") List<ProjectField> projectFields,

                        @Schema(description = "도메인 목록 (ALL, HEALTHCARE, FINTECH, ECOMMERCE, EDUCATION, SOCIAL, ENTERTAINMENT, AI_DATA, ETC), 복수 선택 가능", nullable = true, example = "[\"ECOMMERCE\", \"FINTECH\"]") List<CategoryGenre> categories,

                        @Schema(description = "포지션 목록 (ALL, FRONTEND, BACKEND, INFRA), 복수 선택 가능", nullable = true, example = "[\"BACKEND\", \"FRONTEND\"]") List<ProjectPart> positions,

                        @Schema(description = "기술스택 목록 (JAVA, JAVASCRIPT, REACT, SPRINGBOOT 등), 복수 선택 가능", nullable = true, example = "[\"JAVA\", \"SPRINGBOOT\"]") List<TechName> techstackNames,

                        @Schema(description = "예상 기간 목록 (UNDER_ONE, ONE_TO_THREE, THREE_TO_SIX, SIX_PLUS), 복수 선택 가능", nullable = true, example = "[\"ONE_TO_THREE\", \"THREE_TO_SIX\"]") List<DurationRange> durationRanges,

                        @Schema(description = "페이지 번호 (1부터 시작)", nullable = true, defaultValue = "1", example = "1") @ValidPage Integer page,

                        @Schema(description = "페이지 크기", nullable = true, defaultValue = "10", example = "10") Integer size) {
                public RecommendProjectsPageReq {
                        if (page == null)
                                page = 1;
                        if (size == null)
                                size = 10;
                }

                public Pageable toPageable() {
                        return PageRequest.of(page, size).toPageable();
                }
        }

        public record ChangeStatusReq(
                        @Schema(description = "변경할 상태 (IN_PROGRESS, COMPLETED)", example = "IN_PROGRESS")
                        @NotNull(message = "변경할 상태는 필수입니다.")
                        ProjectStatus status
        ) {}
}