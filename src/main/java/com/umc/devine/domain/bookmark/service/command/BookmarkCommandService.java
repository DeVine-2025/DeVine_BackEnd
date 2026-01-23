package com.umc.devine.domain.bookmark.service.command;

import com.umc.devine.domain.bookmark.dto.BookmarkReqDTO;
import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;

public interface BookmarkCommandService {
    BookmarkResDTO.BookmarkIdDTO createBookmark(Long memberId, BookmarkReqDTO.CreateBookmarkDTO dto);
    BookmarkResDTO.BookmarkIdDTO deleteBookmark(Long memberId, Long bookmarkId);
}
