package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.techstack.enums.TechName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m WHERE m.nickname = :nickname AND m.used = 'ACTIVE'")
    Optional<Member> findByNickname(@Param("nickname") String nickname);

    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.nickname = :nickname AND m.used = 'ACTIVE'")
    boolean existsByNickname(@Param("nickname") String nickname);

    @Query("SELECT DISTINCT m FROM Member m " +
           "WHERE m.mainType = :mainType " +
           "AND m.disclosure = true " +
           "AND m.used = 'ACTIVE' " +
           "AND (:category IS NULL OR EXISTS (SELECT 1 FROM MemberCategory mc WHERE mc.member = m AND mc.category.genre = :category)) " +
           "AND (:techstackName IS NULL OR EXISTS (SELECT 1 FROM DevTechstack dt WHERE dt.member = m AND dt.techstack.name = :techstackName))")
    Page<Member> findDevelopersByFilters(
            @Param("mainType") MemberMainType mainType,
            @Param("category") CategoryGenre category,
            @Param("techstackName") TechName techstackName,
            Pageable pageable);

    @Query("SELECT m FROM Member m WHERE m.clerkId = :clerkId AND m.used = 'ACTIVE'")
    Optional<Member> findByClerkId(@Param("clerkId") String clerkId);

    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.clerkId = :clerkId AND m.used = 'ACTIVE'")
    boolean existsByClerkId(@Param("clerkId") String clerkId);

    @Query("SELECT m FROM Member m WHERE m.mainType = :mainType AND m.used = 'ACTIVE'")
    List<Member> findAllByMainType(@Param("mainType") MemberMainType mainType, Pageable pageable);
}
