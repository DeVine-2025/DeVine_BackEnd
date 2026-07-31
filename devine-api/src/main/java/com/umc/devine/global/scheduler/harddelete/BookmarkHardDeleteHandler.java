package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 북마크는 순수 개인 데이터라 그대로 삭제한다. */
@Component
@Order(90)
@RequiredArgsConstructor
public class BookmarkHardDeleteHandler implements MemberHardDeleteHandler {

    private final BookmarkRepository bookmarkRepository;

    @Override
    public void handle(Member member) {
        bookmarkRepository.bulkDeleteByMember(member);
    }
}
