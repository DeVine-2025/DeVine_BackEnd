package com.umc.devine.domain.bookmark.dto;

import com.umc.devine.domain.bookmark.enums.BookmarkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public class BookmarkReqDTO {

    @Schema(description = "북마크 생성 요청. PROJECT 타입은 targetId 필수, DEVELOPER 타입은 targetNickname 필수")
    public record CreateBookmarkDTO(
            @NotNull(message = "북마크 타입은 필수입니다.")
            @Schema(description = "북마크 대상 타입", example = "PROJECT")
            BookmarkType targetType,

            @Schema(description = "프로젝트 ID (targetType=PROJECT일 때 필수)", example = "1")
            Long targetId,

            @Schema(description = "개발자 닉네임 (targetType=DEVELOPER일 때 필수)", example = "devkim")
            String targetNickname
    ) {
        @AssertTrue(message = "PROJECT 타입은 targetId 필수, DEVELOPER 타입은 targetNickname 필수입니다.")
        @Schema(hidden = true)
        private boolean isValid() {
            if (targetType == null) return true; // @NotNull이 별도 처리
            return (targetType == BookmarkType.PROJECT && targetId != null)
                    || (targetType == BookmarkType.DEVELOPER && targetNickname != null && !targetNickname.isBlank());
        }
    }

    public record DeleteBookmarkDTO(
            @NotNull(message = "북마크 ID는 필수입니다.")
            Long bookmarkId
    ) {}
}
