package com.umc.devine.domain.bookmark.converter;

import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.enums.BookmarkType;

import java.util.List;
import java.util.Map;

public class BookmarkConverter {

    public static BookmarkResDTO.BookmarkDTO toBookmarkDTO(Bookmark bookmark, String resolvedNickname) {
        return BookmarkResDTO.BookmarkDTO.builder()
                .bookmarkId(bookmark.getId())
                .targetType(bookmark.getTargetType())
                .targetId(bookmark.getTargetId())
                .targetNickname(resolvedNickname)
                .createdAt(bookmark.getCreatedAt())
                .build();
    }

    public static BookmarkResDTO.BookmarkListDTO toBookmarkListDTO(List<Bookmark> bookmarks, Map<Long, String> nicknameMap) {
        List<BookmarkResDTO.BookmarkDTO> bookmarkDTOs = bookmarks.stream()
                .map(b -> {
                    String nickname = b.getTargetType() == BookmarkType.DEVELOPER
                            ? nicknameMap.get(b.getTargetId())
                            : null;
                    return toBookmarkDTO(b, nickname);
                })
                .toList();

        return BookmarkResDTO.BookmarkListDTO.builder()
                .bookmarks(bookmarkDTOs)
                .build();
    }

    public static BookmarkResDTO.BookmarkIdDTO toBookmarkIdDTO(Long bookmarkId) {
        return BookmarkResDTO.BookmarkIdDTO.builder()
                .bookmarkId(bookmarkId)
                .build();
    }
}
