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

    /** 회원 탈퇴용: 이 회원이 한 북마크. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Bookmark b WHERE b.member = :member")
    int deleteAllByMemberBulk(@Param("member") Member member);

    /** 회원 탈퇴용: 남이 이 회원(개발자)을 북마크한 행. 프로젝트 북마크는 프로젝트 삭제 함수가 지운다. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Bookmark b WHERE b.targetType = :targetType AND b.targetId = :targetId")
    int deleteAllByTarget(@Param("targetType") BookmarkType targetType, @Param("targetId") Long targetId);

    List<Bookmark> findAllByMember(Member member);

    Optional<Bookmark> findByMemberAndTargetTypeAndTargetId(Member member, BookmarkType targetType, Long targetId);

    boolean existsByMemberAndTargetTypeAndTargetId(Member member, BookmarkType targetType, Long targetId);

    void deleteAllByTargetTypeAndTargetId(BookmarkType targetType, Long targetId);
}
