package com.umc.devine.domain.bookmark.service.query;

import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface BookmarkQueryService {
    BookmarkResDTO.BookmarkListDTO findAllBookmarks(Member member);
}
