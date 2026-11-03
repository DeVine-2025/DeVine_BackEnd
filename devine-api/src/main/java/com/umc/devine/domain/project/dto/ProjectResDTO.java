package com.umc.devine.domain.project.dto;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
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

            @Schema(description = "카테고리", example = "ECOMMERCE")
            CategoryGenre category,

            @Schema(description = "카테고리 이름", example = "이커머스")
            String categoryName,

            @Schema(description = "진행 방식", example = "ONLINE")
            ProjectMode mode,

            @Schema(description = "진행 방식 이름", example = "온라인")
            String modeName,

            @Schema(description = "진행 기간", example = "ONE_TO_THREE")
            DurationRange durationRange,

            @Schema(description = "진행 기간 이름", example = "1~3개월")
            String durationRangeName,

            @Schema(description = "진행 장소", example = "서울 강남구")
            String location,

            @Schema(description = "모집 마감일", example = "2026-01-25")
            LocalDate recruitmentDeadline,

            @Schema(description = "모집 마감까지 남은 일수 (음수면 마감, 0이면 오늘 마감)", example = "5")
            Long daysUntilDeadline,

            @Schema(description = "프로젝트 제목", example = "AI 기반 협업 플랫폼")
            String title,

            @Schema(description = "프로젝트 내용", example = "GitHub 분석 기반 개발자 매칭 서비스를 개발합니다.")
            String content,

            @Schema(description = "프로젝트 상태", example = "RECRUITING")
            ProjectStatus status,

            @Schema(description = "프로젝트 생성자 ID", example = "1")
            Long creatorId,

            @Schema(description = "프로젝트 생성자 닉네임", example = "김개발")
            String creatorNickname,

            @Schema(description = "모집 분야 목록")
            List<RecruitmentInfo> recruitments,

            @Schema(description = "프로젝트 이미지 목록")
            List<ImageInfo> images
    ) {}

    @Builder
    public record UpdateProjectRes(
            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "프로젝트 분야", example = "WEB")
            ProjectField projectField,

            @Schema(description = "프로젝트 분야 이름", example = "웹")
            String projectFieldName,

            @Schema(description = "카테고리", example = "ECOMMERCE")
            CategoryGenre category,

            @Schema(description = "카테고리 이름", example = "이커머스")
            String categoryName,

            @Schema(description = "진행 방식", example = "ONLINE")
            ProjectMode mode,

            @Schema(description = "진행 방식 이름", example = "온라인")
            String modeName,

            @Schema(description = "진행 기간", example = "ONE_TO_THREE")
            DurationRange durationRange,

            @Schema(description = "진행 기간 이름", example = "1~3개월")
            String durationRangeName,

            @Schema(description = "진행 장소", example = "서울 강남구")
            String location,

            @Schema(description = "모집 마감일", example = "2026-01-25")
            LocalDate recruitmentDeadline,

            @Schema(description = "모집 마감까지 남은 일수 (음수면 마감, 0이면 오늘 마감)", example = "5")
            Long daysUntilDeadline,

            @Schema(description = "프로젝트 제목", example = "수정된 AI 협업 플랫폼")
            String title,

            @Schema(description = "프로젝트 내용", example = "수정된 내용입니다.")
            String content,

            @Schema(description = "프로젝트 상태", example = "RECRUITING")
            ProjectStatus status,

            @Schema(description = "프로젝트 생성자 ID", example = "1")
            Long creatorId,

            @Schema(description = "프로젝트 생성자 닉네임", example = "김개발")
            String creatorNickname,

            @Schema(description = "프로젝트 생성자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
            String creatorImage,

            @Schema(description = "프로젝트 생성자 Clerk ID (채팅방 생성 시 사용)", example = "user_2abc123xyz")
            String creatorClerkId,

            @Schema(description = "모집 분야 목록")
            List<RecruitmentInfo> recruitments,

            @Schema(description = "프로젝트 이미지 목록")
            List<ImageInfo> images,

            @Schema(description = "유저 화면 노출 여부. false면 신고 처리 등으로 관리자가 비노출 처리한 상태다. "
                    + "비노출 프로젝트는 작성자 본인만 조회할 수 있고 수정/삭제/상태변경은 불가하다.", example = "true")
            boolean visible
    ) {}

    @Builder
    public record ImageInfo(
            @Schema(description = "이미지 ID", example = "1")
            Long imageId,

            @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
            String imageUrl
    ) {}

    @Builder
    public record RecruitmentInfo(
            @Schema(description = "포지션", example = "BACKEND")
            ProjectPart position,

            @Schema(description = "포지션 이름", example = "백엔드")
            String positionName,

            @Schema(description = "모집 인원", example = "2")
            Integer count,

            @Schema(description = "현재 모집된 인원", example = "0")
            Integer currentCount,

            @Schema(description = "기술 스택 목록")
            List<TechStackInfo> techStacks
    ) {}

    @Builder
    public record TechStackInfo(
            @Schema(description = "기술 스택", example = "JAVA")
            TechName techStack
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

            @Schema(description = "카테고리", example = "ECOMMERCE")
            CategoryGenre category,

            @Schema(description = "카테고리 이름", example = "이커머스")
            String categoryName,

            @Schema(description = "진행 방식", example = "ONLINE")
            ProjectMode mode,

            @Schema(description = "진행 방식 이름", example = "온라인")
            String modeName,

            @Schema(description = "진행 기간", example = "ONE_TO_THREE")
            DurationRange durationRange,

            @Schema(description = "진행 기간 이름", example = "1~3개월")
            String durationRangeName,

            @Schema(description = "진행 장소", example = "서울 강남구")
            String location,

            @Schema(description = "모집 마감일", example = "2026-01-25")
            LocalDate recruitmentDeadline,

            @Schema(description = "모집 마감까지 남은 일수 (음수면 마감, 0이면 오늘 마감)", example = "5")
            Long daysUntilDeadline,

            @Schema(description = "프로젝트 상태")
            ProjectStatus status,

            @Schema(description = "썸네일 이미지 URL")
            String thumbnailUrl,

            @Schema(description = "모집 포지션 목록 (간략 정보)")
            List<PositionSummary> positions,

            @Schema(description = "생성자 닉네임", example = "김개발")
            String creatorNickname
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

            @Schema(description = "카테고리", example = "ECOMMERCE")
            CategoryGenre category,

            @Schema(description = "카테고리 이름", example = "이커머스")
            String categoryName,

            @Schema(description = "진행 방식", example = "ONLINE")
            ProjectMode mode,

            @Schema(description = "진행 방식 이름", example = "온라인")
            String modeName,

            @Schema(description = "진행 기간", example = "ONE_TO_THREE")
            DurationRange durationRange,

            @Schema(description = "진행 기간 이름", example = "1~3개월")
            String durationRangeName,

            @Schema(description = "진행 장소", example = "서울 강남구")
            String location,

            @Schema(description = "모집 마감일", example = "2026-01-25")
            LocalDate recruitmentDeadline,

            @Schema(description = "모집 마감까지 남은 일수 (음수면 마감, 0이면 오늘 마감)", example = "5")
            Long daysUntilDeadline,

            @Schema(description = "프로젝트 상태")
            ProjectStatus status,

            @Schema(description = "썸네일 이미지 URL")
            String thumbnailUrl,

            @Schema(description = "모집 포지션 목록 (간략 정보)")
            List<PositionSummary> positions,

            @Schema(description = "생성자 닉네임", example = "김개발")
            String creatorNickname,

            @Schema(description = "총 추천 점수 (100점 만점)", example = "83.0")
            Double totalScore,

            @Schema(description = "리포트 유사도 (100점 만점)", example = "80.0")
            Double similarityScorePercent,

            @Schema(description = "기술 스택 일치도 (100점 만점)", example = "75.0")
            Double techstackScorePercent,

            @Schema(description = "도메인 일치 여부", example = "true")
            Boolean domainMatch
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

            @Schema(description = "기술 스택 목록")
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
            @Schema(description = "추천 프로젝트 목록")
            List<RecommendedProjectSummary> projects,

            @Schema(description = "반환된 프로젝트 수")
            Integer count
    ) {}

    // 내 프로젝트 정보 (PM/개발자 공통)
    @Builder
    public record MyProjectInfo(
            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "프로젝트 제목", example = "AI 협업 플랫폼")
            String title,

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

            @Schema(description = "내 파트 (개발자인 경우, PM은 null)", example = "BACKEND")
            ProjectPart myPart,

            @Schema(description = "내 파트 이름 (개발자인 경우, PM은 null)", example = "백엔드")
            String myPartName,

            @Schema(description = "프로젝트 상태", example = "IN_PROGRESS")
            ProjectStatus projectStatus,

            @Schema(description = "프로젝트 상태 이름", example = "진행 중")
            String projectStatusName,

            @Schema(description = "유저 화면 노출 여부. false면 신고 처리 등으로 관리자가 비노출 처리한 상태로, "
                    + "작성자 본인에게만 보이고 검색/추천 등 다른 경로에는 노출되지 않는다.", example = "true")
            boolean visible
    ) {}

    // 내 프로젝트 목록 응답 (PM/개발자 공통)
    @Builder
    public record MyProjectsRes(
            @Schema(description = "내 프로젝트 목록")
            PagedResponse<MyProjectInfo> projects
    ) {}

    // PM 본인용 프로젝트 관리 정보: 상태와 대기 건으로 누를 수 있는 버튼을 계산한다
    @Builder
    public record ManageRes(
            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "프로젝트 상태", example = "DEADLINE_PASSED")
            ProjectStatus status,

            @Schema(description = "프로젝트 상태 이름", example = "마감일 지남")
            String statusName,

            @Schema(description = "숨김 여부", example = "false")
            boolean hidden,

            @Schema(description = "관리자가 숨긴 프로젝트인지 (PM이 다시 보이게 할 수 없음)", example = "false")
            boolean hiddenByAdmin,

            @Schema(description = "대기 중인 지원·제안 수", example = "2")
            long pendingCount,

            @Schema(description = "대기 중인 지원·제안 목록 (오래된 순)")
            List<PendingMatchingInfo> pendingMatchings,

            @Schema(description = "지금 마감할 수 있는지 (모집 중·마감일 지남 + 대기 0건)", example = "false")
            boolean canClose,

            @Schema(description = "지금 삭제할 수 있는지 (마감·진행 중·완료 + 대기 0건)", example = "false")
            boolean canDelete,

            @Schema(description = "숨기기를 켜고 끌 수 있는지 (관리자 숨김이면 false)", example = "true")
            boolean canToggleHidden
    ) {}

    @Builder
    public record PendingMatchingInfo(
            @Schema(description = "매칭 ID", example = "10")
            Long matchingId,

            @Schema(description = "APPLY: 받은 지원(수락·거절), PROPOSE: 보낸 제안(제안 취소)", example = "APPLY")
            MatchingType matchingType,

            @Schema(description = "지원자 또는 제안받은 개발자 닉네임", example = "devuser")
            String nickname,

            @Schema(description = "파트", example = "BACKEND")
            ProjectPart part
    ) {}
}
