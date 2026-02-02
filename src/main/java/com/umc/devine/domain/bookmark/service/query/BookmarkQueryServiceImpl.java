package com.umc.devine.domain.bookmark.service.query;

import com.umc.devine.domain.bookmark.converter.BookmarkConverter;
import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkQueryServiceImpl implements BookmarkQueryService {

    private final BookmarkRepository bookmarkRepository;

    @Override
    public BookmarkResDTO.BookmarkListDTO findAllBookmarks(Member member) {
        List<Bookmark> bookmarks = bookmarkRepository.findAllByMember(member);

        return BookmarkConverter.toBookmarkListDTO(bookmarks);
    }
}
