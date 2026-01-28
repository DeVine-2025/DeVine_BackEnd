package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.techstack.enums.TechGenre;
import com.umc.devine.domain.techstack.enums.TechName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByNickname(String nickname);
    boolean existsByNickname(String nickname);

    @Query("SELECT DISTINCT m FROM Member m " +
           "LEFT JOIN DevTechstack dt ON dt.member = m " +
           "LEFT JOIN MemberCategory mc ON mc.member = m " +
           "WHERE m.mainType = :mainType " +
           "AND m.disclosure = true " +
           "AND (:category IS NULL OR mc.category.genre = :category) " +
           "AND (:techGenre IS NULL OR dt.techstack.genre = :techGenre) " +
           "AND (:techstackName IS NULL OR dt.techstack.name = :techstackName)")
    Page<Member> findDevelopersByFilters(
            @Param("mainType") MemberMainType mainType,
            @Param("category") CategoryGenre category,
            @Param("techGenre") TechGenre techGenre,
            @Param("techstackName") TechName techstackName,
            Pageable pageable);

    Optional<Member> findByClerkId(String clerkId);
    boolean existsByClerkId(String clerkId);

    List<Member> findAllByMainType(MemberMainType mainType, Pageable pageable);
}
