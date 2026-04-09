package com.umc.devine.domain.bookmark.service.query;

import com.umc.devine.domain.bookmark.converter.BookmarkConverter;
import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkQueryServiceImpl implements BookmarkQueryService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;

    @Override
    public BookmarkResDTO.BookmarkListDTO findAllBookmarks(Member member) {
        List<Bookmark> bookmarks = bookmarkRepository.findAllByMember(member);

        // DEVELOPER 타입 북마크의 targetId를 일괄 조회하여 nickname 매핑
        List<Long> developerTargetIds = bookmarks.stream()
                .filter(b -> b.getTargetType() == BookmarkType.DEVELOPER)
                .map(Bookmark::getTargetId)
                .distinct()
                .toList();

        Map<Long, String> nicknameMap = memberRepository.findAllById(developerTargetIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getNickname));

        return BookmarkConverter.toBookmarkListDTO(bookmarks, nicknameMap);
    }
}
