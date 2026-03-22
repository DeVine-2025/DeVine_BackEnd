package com.umc.devine.domain.bookmark.dto;

import com.umc.devine.domain.bookmark.enums.BookmarkType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class BookmarkResDTO {

    @Builder
    @Schema(description = "북마크 정보. PROJECT 타입은 targetId, DEVELOPER 타입은 targetNickname 반환")
    public record BookmarkDTO(
            @Schema(description = "북마크 ID", example = "1")
            Long bookmarkId,

            @Schema(description = "북마크 대상 타입", example = "PROJECT")
            BookmarkType targetType,

            @Schema(description = "프로젝트 ID (PROJECT 타입일 때)", example = "1", nullable = true)
            Long targetId,

            @Schema(description = "개발자 닉네임 (DEVELOPER 타입일 때)", example = "devkim", nullable = true)
            String targetNickname,

            @Schema(description = "생성 일시")
            LocalDateTime createdAt
    ) {}

    @Builder
    public record BookmarkListDTO(
            @Schema(description = "북마크 목록")
            List<BookmarkDTO> bookmarks
    ) {}

    @Builder
    public record BookmarkIdDTO(
            @Schema(description = "북마크 ID", example = "1")
            Long bookmarkId
    ) {}
}
