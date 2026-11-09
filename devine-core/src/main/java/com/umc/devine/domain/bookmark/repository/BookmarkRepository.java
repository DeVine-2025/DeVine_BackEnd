package com.umc.devine.domain.bookmark.repository;

import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Bookmark b WHERE b.member = :member")
    int deleteAllByMemberBulk(@Param("member") Member member);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Bookmark b WHERE b.targetType = :targetType AND b.targetId = :targetId")
    int deleteAllByTarget(@Param("targetType") BookmarkType targetType, @Param("targetId") Long targetId);

    List<Bookmark> findAllByMember(Member member);

    Optional<Bookmark> findByMemberAndTargetTypeAndTargetId(Member member, BookmarkType targetType, Long targetId);

    boolean existsByMemberAndTargetTypeAndTargetId(Member member, BookmarkType targetType, Long targetId);

    void deleteAllByTargetTypeAndTargetId(BookmarkType targetType, Long targetId);
}
