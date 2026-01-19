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
    private final MemberRepository memberRepository;

    @Override
    public BookmarkResDTO.BookmarkListDTO findAllBookmarks(Long memberId) {
        // TODO 토큰 방식 적용 후 시큐리티 컨텍스트에서 꺼내 쓰도록 수정
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        List<Bookmark> bookmarks = bookmarkRepository.findAllByMember(member);

        return BookmarkConverter.toBookmarkListDTO(bookmarks);
    }
}
