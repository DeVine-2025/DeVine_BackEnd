package com.umc.devine.domain.bookmark.dto;

import com.umc.devine.domain.bookmark.enums.BookmarkType;
import jakarta.validation.constraints.NotNull;

public class BookmarkReqDTO {

    public record CreateBookmarkDTO(
            @NotNull(message = "북마크 타입은 필수입니다.")
            BookmarkType targetType,

            @NotNull(message = "대상 ID는 필수입니다.")
            Long targetId
    ) {}

    public record DeleteBookmarkDTO(
            @NotNull(message = "북마크 ID는 필수입니다.")
            Long bookmarkId
    ) {}
}
