package com.umc.devine.domain.bookmark.service.command;

import com.umc.devine.domain.bookmark.dto.BookmarkReqDTO;
import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface BookmarkCommandService {
    BookmarkResDTO.BookmarkIdDTO createBookmark(Member member, BookmarkReqDTO.CreateBookmarkDTO dto);
    BookmarkResDTO.BookmarkIdDTO deleteBookmark(Member member, Long bookmarkId);
}
