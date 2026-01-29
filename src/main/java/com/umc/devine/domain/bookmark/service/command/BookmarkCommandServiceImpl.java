package com.umc.devine.domain.bookmark.service.command;

import com.umc.devine.domain.bookmark.converter.BookmarkConverter;
import com.umc.devine.domain.bookmark.dto.BookmarkReqDTO;
import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.exception.BookmarkException;
import com.umc.devine.domain.bookmark.exception.code.BookmarkErrorCode;
import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkCommandServiceImpl implements BookmarkCommandService {

    private final BookmarkRepository bookmarkRepository;

    @Override
    public BookmarkResDTO.BookmarkIdDTO createBookmark(Member member, BookmarkReqDTO.CreateBookmarkDTO dto) {
        if (bookmarkRepository.existsByMemberAndTargetTypeAndTargetId(member, dto.targetType(), dto.targetId())) {
            throw new BookmarkException(BookmarkErrorCode.ALREADY_EXISTS);
        }

        Bookmark bookmark = BookmarkConverter.toBookmark(member, dto);
        Bookmark savedBookmark = bookmarkRepository.save(bookmark);

        return BookmarkConverter.toBookmarkIdDTO(savedBookmark.getId());
    }

    @Override
    public BookmarkResDTO.BookmarkIdDTO deleteBookmark(Member member, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new BookmarkException(BookmarkErrorCode.NOT_FOUND));

        if (!bookmark.getMember().getId().equals(member.getId())) {
            throw new BookmarkException(BookmarkErrorCode.FORBIDDEN);
        }

        bookmarkRepository.delete(bookmark);

        return BookmarkConverter.toBookmarkIdDTO(bookmarkId);
    }
}
