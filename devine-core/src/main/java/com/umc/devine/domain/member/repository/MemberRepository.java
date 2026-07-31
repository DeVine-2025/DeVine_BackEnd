package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.querydsl.MemberQueryDsl;
import com.umc.devine.domain.techstack.enums.TechName;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberQueryDsl {

    @Query("SELECT m FROM Member m WHERE m.nickname = :nickname AND m.used = 'ACTIVE'")
    Optional<Member> findByNickname(@Param("nickname") String nickname);

    /** 관리자용 조회. 정지/강제탈퇴예정 등 ACTIVE가 아닌 회원도 찾아야 하므로 상태로 거르지 않는다. */
    @Query("SELECT m FROM Member m WHERE m.nickname = :nickname")
    Optional<Member> findByNicknameIncludingInactive(@Param("nickname") String nickname);

    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.nickname = :nickname AND m.used = 'ACTIVE'")
    boolean existsByNickname(@Param("nickname") String nickname);

    @Query("SELECT DISTINCT m FROM Member m " +
           "WHERE m.disclosure = true " +
           "AND m.used = 'ACTIVE' " +
           "AND (:categories IS NULL OR EXISTS (SELECT 1 FROM MemberCategory mc WHERE mc.member = m AND mc.category.genre IN :categories)) " +
           "AND (:techstackNames IS NULL OR EXISTS (SELECT 1 FROM DevTechstack dt WHERE dt.member = m AND dt.techstack.name IN :techstackNames)) " +
           "ORDER BY m.createdAt DESC")
    Page<Member> findDevelopersByFilters(
            @Param("categories") List<CategoryGenre> categories,
            @Param("techstackNames") List<TechName> techstackNames,
            Pageable pageable);

    @Query("SELECT m FROM Member m WHERE m.clerkId = :clerkId AND m.used = 'ACTIVE'")
    Optional<Member> findByClerkId(@Param("clerkId") String clerkId);

    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.clerkId = :clerkId AND m.used = 'ACTIVE'")
    boolean existsByClerkId(@Param("clerkId") String clerkId);

    @Query("SELECT m.id FROM Member m WHERE m.clerkId = :clerkId AND m.used = 'ACTIVE'")
    Optional<Long> findIdByClerkId(@Param("clerkId") String clerkId);

    List<Member> findByUsedAndScheduledWithdrawalAtBefore(MemberStatus used, LocalDateTime threshold);

    List<Member> findByUsedAndDeletedAtBefore(MemberStatus used, LocalDateTime threshold);

    @Query("SELECT m FROM Member m WHERE m.used = 'ACTIVE'")
    List<Member> findAllActive();

    @Query("SELECT m FROM Member m WHERE m.nickname IN :nicknames AND m.used = 'ACTIVE'")
    List<Member> findAllByNicknameIn(@Param("nicknames") List<String> nicknames);

    /** 탈퇴 등 동시 요청 경쟁 조건을 막기 위한 행 잠금 조회. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);
}
