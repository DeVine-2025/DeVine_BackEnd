package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.MemberBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberBlockRepository extends JpaRepository<MemberBlock, Long> {
    boolean existsByBlockerIdAndTargetId(Long blockerId, Long targetId);

    @Query("SELECT COUNT(b) > 0 FROM MemberBlock b WHERE " +
            "(b.blocker.id = :firstId AND b.target.id = :secondId) OR " +
            "(b.blocker.id = :secondId AND b.target.id = :firstId)")
    boolean existsBetween(@Param("firstId") Long firstId, @Param("secondId") Long secondId);

    @Query("SELECT DISTINCT CASE WHEN b.blocker.id = :memberId THEN b.target.id ELSE b.blocker.id END " +
            "FROM MemberBlock b WHERE b.blocker.id = :memberId OR b.target.id = :memberId")
    List<Long> findExcludedMemberIds(@Param("memberId") Long memberId);

    @Query(value = "SELECT b FROM MemberBlock b JOIN FETCH b.target WHERE b.blocker.id = :memberId ORDER BY b.createdAt DESC, b.id DESC",
            countQuery = "SELECT COUNT(b) FROM MemberBlock b WHERE b.blocker.id = :memberId")
    Page<MemberBlock> findByBlockerId(@Param("memberId") Long memberId, Pageable pageable);

    void deleteByBlockerIdAndTargetId(Long blockerId, Long targetId);
}
