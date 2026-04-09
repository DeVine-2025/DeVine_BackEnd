package com.umc.devine.domain.bookmark.repository;

import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findAllByMember(Member member);

    Optional<Bookmark> findByMemberAndTargetTypeAndTargetId(Member member, BookmarkType targetType, Long targetId);

    boolean existsByMemberAndTargetTypeAndTargetId(Member member, BookmarkType targetType, Long targetId);
}
