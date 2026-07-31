package com.umc.devine.domain.bookmark.repository;

import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findAllByMember(Member member);

    Optional<Bookmark> findByMemberAndTargetTypeAndTargetId(Member member, BookmarkType targetType, Long targetId);

    boolean existsByMemberAndTargetTypeAndTargetId(Member member, BookmarkType targetType, Long targetId);

    /** Hard Delete 배치에서 회원 행을 지우기 전에 호출한다. 순수 개인 데이터라 다른 회원에게 영향 없다. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Bookmark b WHERE b.member = :member")
    int bulkDeleteByMember(@Param("member") Member member);
}
