package com.umc.devine.domain.member.dto;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.validation.annotation.ValidNickname;
import io.swagger.v3.oas.annotations.media.Schema;
import com.umc.devine.domain.member.enums.ContactType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class MemberReqDTO {

    @Builder
    @Schema(description = "회원가입 요청")
    public record SignupDTO(
            @Schema(description = "약관 동의 목록", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotEmpty(message = "약관 동의 정보는 필수입니다.")
            @Valid
            List<AgreementDTO> agreements,

            @Schema(description = "닉네임", requiredMode = Schema.RequiredMode.REQUIRED, example = "devine")
            @NotNull(message = "닉네임은 필수입니다.")
            @ValidNickname
            String nickname,

            @Schema(description = "프로필 이미지 URL", example = "https://example.com/image.jpg")
            @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다.")
            String imageUrl,

            @Schema(description = "역할 (PM / DEVELOPER)", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEVELOPER")
            @NotNull(message = "역할 선택은 필수입니다.")
            MemberMainType mainType,

            @Schema(description = "관심 도메인 ID 목록 (1~3개)", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1, 2]")
            @NotEmpty(message = "관심 도메인은 최소 1개 이상 선택해야 합니다.")
            @Size(min = 1, max = 3, message = "관심 도메인은 1개 이상 3개 이하로 선택해야 합니다.")
            List<Long> categoryIds,

            @Schema(description = "보유 기술 스택 ID 목록", example = "[1, 2, 3]")
            @Size(max = 50, message = "기술 스택은 최대 50개까지 선택할 수 있습니다.")
            List<Long> techstackIds,

            @Schema(description = "한줄 소개", example = "열정적인 백엔드 개발자입니다.")
            @Size(max = 255, message = "한줄 소개는 255자 이하여야 합니다.")
            String body,

            @Schema(description = "이메일", example = "user@example.com")
            @Email(message = "올바른 이메일 형식이어야 합니다.")
            @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
            String email,

            @Schema(description = "링크드인 주소", example = "https://linkedin.com/in/username")
            @Size(max = 500, message = "링크드인 주소는 500자 이하여야 합니다.")
            String linkedin
    ) {}

    @Builder
    @Schema(description = "약관 동의 정보")
    public record AgreementDTO(
            @Schema(description = "약관 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
            @NotNull(message = "약관 ID는 필수입니다.")
            Long termsId,

            @Schema(description = "동의 여부", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
            @NotNull(message = "동의 여부는 필수입니다.")
            Boolean agreed
    ) {}

    @Builder
    @Schema(description = "연락처 정보")
    public record ContactDTO(
            @Schema(description = "연락처 유형", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "연락처 유형은 필수입니다.")
            ContactType type,

            @Schema(description = "연락처 값", example = "user@example.com")
            @Size(max = 100, message = "연락처 값은 100자 이하여야 합니다.")
            String value,

            @Schema(description = "연락처 링크", example = "https://github.com/username")
            @Size(max = 500, message = "연락처 링크는 500자 이하여야 합니다.")
            String link
    ) {}
    @Builder
    public record UpdateMemberDTO(
            @Schema(description = "닉네임", nullable = true, example = "devine")
            @ValidNickname
            String nickname,

            @Schema(description = "이미지 url", nullable = true, example = "https://devine.com/image.jpg")
            @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
            String imageUrl,

            @Schema(description = "주소", nullable = true, example = "서울 구로구")
            @Size(max = 100, message = "주소는 100자 이하여야 합니다.")
            String address,

            @Schema(description = "자기소개", nullable = true, example = "자기 소개 내용입니다.")
            @Size(max = 255, message = "자기소개는 255자 이하여야 합니다.")
            String body,

            @Schema(description = "도메인", nullable = true)
            @Size(max = 3, message = "도메인은 최대 3개까지 선택할 수 있습니다.")
            CategoryGenre[] domains,

            @Schema(description = "연락처", nullable = true)
            @Valid
            @Size(max = 10, message = "연락처는 최대 10개까지 등록할 수 있습니다.")
            ContactDTO[] contacts,

            @Schema(description = "메인권한", nullable = true)
            MemberMainType mainType,

            @Schema(description = "개발자 검색 노출 공개", nullable = true)
            Boolean disclosure,

            @Schema(description = "프로젝트 제안 알림받기", nullable = true)
            Boolean proposalAlarm
    ) {}

    @Builder
    public record AddTechstackDTO(
            @Schema(description = "추가할 기술 스택 ID 목록", required = true)
            @NotEmpty(message = "기술 스택 ID 목록은 필수입니다.")
            @Size(max = 50, message = "기술 스택은 최대 50개까지 추가할 수 있습니다.")
            Long[] techstackIds
    ) {}

    @Builder
    public record RemoveTechstackDTO(
            @Schema(description = "삭제할 기술 스택 ID 목록", required = true)
            @NotEmpty(message = "기술 스택 ID 목록은 필수입니다.")
            @Size(max = 50, message = "기술 스택은 최대 50개까지 삭제할 수 있습니다.")
            Long[] techstackIds,
            @Schema(description = "삭제할 기술 종류", required = false)
            TechstackSource source
    ) {}

    @Builder
    public record RecommendDeveloperDTO(
            @Schema(description = "프로젝트 ID (선택, 없으면 전체 공개 개발자 조회)", nullable = true)
            Long projectId,

            @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            Integer page,

            @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            Integer size
    ) {
        public RecommendDeveloperDTO {
            if (page == null) page = 1;
            if (size == null) size = 10;
        }

        public Pageable toPageable() {
            return PageRequest.of(page, size).toPageable();
        }
    }

    @Builder
    public record SearchDeveloperDTO(
            @Schema(description = "카테고리 목록 (HEALTHCARE, FINTECH, ECOMMERCE, EDUCATION, SOCIAL, ENTERTAINMENT, AI_DATA, ETC), 복수 선택 가능", nullable = true)
            List<CategoryGenre> categories,

            @Schema(description = "기술 스택 이름 목록 (JAVA, JAVASCRIPT, REACT, SPRINGBOOT 등), 복수 선택 가능", nullable = true)
            List<TechName> techstackNames,

            @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            Integer page,

            @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            Integer size
    ) {
        public SearchDeveloperDTO {
            if (page == null) page = 1;
            if (size == null) size = 10;
        }

        public Pageable toPageable() {
            return PageRequest.of(page, size).toPageable();
        }
    }

    @Builder
    @Schema(description = "특정 회원 프로젝트 목록 조회 요청")
    public record MemberProjectSearchDTO(
            @Schema(description = "프로젝트 상태 필터 (RECRUITING, IN_PROGRESS, COMPLETED 등), 복수 선택 가능", nullable = true)
            List<ProjectStatus> statuses,

            @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            Integer page,

            @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            Integer size
    ) {
        public MemberProjectSearchDTO {
            if (page == null) page = 1;
            if (size == null) size = 10;
        }

        public Pageable toPageable() {
            return PageRequest.of(page, size).toPageable();
        }
    }

    @Builder
    @Schema(description = "GitHub 레포지토리 동기화 요청")
    public record GitRepoSyncDTO(
            @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            Integer page,

            @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            Integer size
    ) {
        public GitRepoSyncDTO {
            if (page == null) page = 1;
            if (size == null) size = 10;
        }

        public Pageable toPageable() {
            return PageRequest.of(page, size).toPageable();
        }
    }
}
