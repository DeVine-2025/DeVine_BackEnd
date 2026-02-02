package com.umc.devine.domain.member.dto;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.techstack.enums.TechGenre;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.global.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import org.springframework.data.domain.Pageable;

public class MemberReqDTO {
    @Builder
    public record UpdateMemberDTO(
            @Schema(description = "닉네임", nullable = true, example = "devine") String nickname,
            @Schema(description = "이미지 url", nullable = true, example = "https://devine.com/image.jpg") String imageUrl,
            @Schema(description = "주소", nullable = true, example = "서울 구로구") String address,
            @Schema(description = "자기소개", nullable = true, example = "자기 소개 내용입니다.") String body,
            @Schema(description = "도메인", nullable = true) CategoryGenre[] domains,
            @Schema(description = "연락처", nullable = true) @Valid MemberResDTO.ContactDTO[] contacts,
            @Schema(description = "메인권한", nullable = true) MemberMainType mainType,
            @Schema(description = "개발자 검색 노출 공개", nullable = true) Boolean disclosure
            ) {}

    @Builder
    public record AddTechstackDTO(
            @Schema(description = "추가할 기술 스택 ID 목록", required = true)
            @NotEmpty(message = "기술 스택 ID 목록은 필수입니다.")
            Long[] techstackIds
    ) {}

    @Builder
    public record RemoveTechstackDTO(
            @Schema(description = "삭제할 기술 스택 ID 목록", required = true)
            @NotEmpty(message = "기술 스택 ID 목록은 필수입니다.")
            Long[] techstackIds,
            @Schema(description = "삭제할 기술 종류", required = false)
            TechstackSource source
    ) {}

    @Builder
    public record RecommendDeveloperDTO(
            @Schema(description = "프로젝트 ID 목록", nullable = true)
            Long[] projectIds,

            @Schema(description = "도메인 (HEALTH, ECOMMERCE, FINANCE, EDUCATION, ENTERTAINMENT, ETC)", nullable = true)
            CategoryGenre category,

            @Schema(description = "기술 장르 (LANGUAGE, FRAMEWORK, DATABASE, CLOUD, CONTAINER, MOBILE)", nullable = true)
            TechGenre techGenre,

            @Schema(description = "기술 스택 이름 (JAVA, JAVASCRIPT, REACT, SPRING 등)", nullable = true)
            TechName techstackName,

            @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
            Integer page,

            @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
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
            @Schema(description = "카테고리 (HEALTH, ECOMMERCE, FINANCE, EDUCATION, ENTERTAINMENT, ETC)", nullable = true)
            CategoryGenre category,

            @Schema(description = "기술 장르 (LANGUAGE, FRAMEWORK, DATABASE, CLOUD, CONTAINER, MOBILE)", nullable = true)
            TechGenre techGenre,

            @Schema(description = "기술 스택 이름 (JAVA, JAVASCRIPT, REACT, SPRING 등)", nullable = true)
            TechName techstackName,

            @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
            Integer page,

            @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
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
}
