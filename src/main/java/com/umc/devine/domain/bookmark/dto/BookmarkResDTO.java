package com.umc.devine.domain.bookmark.dto;

import com.umc.devine.domain.bookmark.enums.BookmarkType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class BookmarkResDTO {

    @Builder
    public record BookmarkDTO(
            Long bookmarkId,
            BookmarkType targetType,
            Long targetId,
            LocalDateTime createdAt
    ) {}

    @Builder
    public record BookmarkListDTO(
            List<BookmarkDTO> bookmarks
    ) {}

    @Builder
    public record BookmarkIdDTO(
            Long bookmarkId
    ) {}
}
