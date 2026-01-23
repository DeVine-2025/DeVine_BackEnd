package com.umc.devine.domain.bookmark.converter;

import com.umc.devine.domain.bookmark.dto.BookmarkReqDTO;
import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.member.entity.Member;

import java.util.List;
import java.util.stream.Collectors;

public class BookmarkConverter {

    public static Bookmark toBookmark(Member member, BookmarkReqDTO.CreateBookmarkDTO dto) {
        return Bookmark.builder()
                .member(member)
                .targetType(dto.targetType())
                .targetId(dto.targetId())
                .build();
    }

    public static BookmarkResDTO.BookmarkDTO toBookmarkDTO(Bookmark bookmark) {
        return BookmarkResDTO.BookmarkDTO.builder()
                .bookmarkId(bookmark.getId())
                .targetType(bookmark.getTargetType())
                .targetId(bookmark.getTargetId())
                .createdAt(bookmark.getCreatedAt())
                .build();
    }

    public static BookmarkResDTO.BookmarkListDTO toBookmarkListDTO(List<Bookmark> bookmarks) {
        List<BookmarkResDTO.BookmarkDTO> bookmarkDTOs = bookmarks.stream()
                .map(BookmarkConverter::toBookmarkDTO)
                .collect(Collectors.toList());

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
