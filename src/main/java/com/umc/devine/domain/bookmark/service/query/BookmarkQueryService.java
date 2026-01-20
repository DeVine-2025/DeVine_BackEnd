package com.umc.devine.domain.bookmark.service.query;

import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;

public interface BookmarkQueryService {
    BookmarkResDTO.BookmarkListDTO findAllBookmarks(Long memberId);
}
