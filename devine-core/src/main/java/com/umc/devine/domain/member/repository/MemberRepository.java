package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.enums.TechName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m WHERE m.nickname = :nickname AND m.used = 'ACTIVE'")
    Optional<Member> findByNickname(@Param("nickname") String nickname);

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

    /**
     * Hard delete 배치용. DELETED 상태이고 deletedAt 이 임계 시각 이전인 회원을 페이징 조회.
     */
    @Query("SELECT m FROM Member m WHERE m.used = 'DELETED' AND m.deletedAt IS NOT NULL AND m.deletedAt < :threshold ORDER BY m.deletedAt ASC")
    List<Member> findDeletedBefore(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    // ── Hard delete 배치용 native 정리 쿼리 ──
    // withdraw() 에서 PII 데이터(contact, git_repo_url, dev_techstack, dev_report, report_embedding)는
    // 이미 삭제하지만, 구버전 데이터 또는 삭제 누락 대비 방어적으로 포함한다.

    @Modifying @Query(value = "DELETE FROM report_embedding WHERE dev_report_id IN (SELECT dev_report_id FROM dev_report WHERE git_repo_id IN (SELECT git_repo_id FROM git_repo_url WHERE member_id = :id))", nativeQuery = true)
    int hardDeleteReportEmbeddingsOf(@Param("id") Long id);

    @Modifying @Query(value = "DELETE FROM dev_report WHERE git_repo_id IN (SELECT git_repo_id FROM git_repo_url WHERE member_id = :id)", nativeQuery = true)
    int hardDeleteDevReportsOf(@Param("id") Long id);

    @Modifying @Query(value = "DELETE FROM git_repo_url WHERE member_id = :id", nativeQuery = true)
    int hardDeleteGitRepoUrlsOf(@Param("id") Long id);

    @Modifying @Query(value = "DELETE FROM contact WHERE member_id = :id", nativeQuery = true)
    int hardDeleteContactsOf(@Param("id") Long id);

    @Modifying @Query(value = "DELETE FROM dev_techstack WHERE member_id = :id", nativeQuery = true)
    int hardDeleteDevTechstacksOf(@Param("id") Long id);

    @Modifying @Query(value = "DELETE FROM member_category WHERE member_id = :id", nativeQuery = true)
    int hardDeleteMemberCategoriesOf(@Param("id") Long id);

    @Modifying @Query(value = "DELETE FROM member_agreement WHERE member_id = :id", nativeQuery = true)
    int hardDeleteMemberAgreementsOf(@Param("id") Long id);

    @Modifying @Query(value = "DELETE FROM bookmark WHERE member_id = :id", nativeQuery = true)
    int hardDeleteBookmarksOf(@Param("id") Long id);

    @Modifying @Query(value = "DELETE FROM member_report_credit WHERE member_id = :id", nativeQuery = true)
    int hardDeleteMemberReportCreditsOf(@Param("id") Long id);

    @Modifying @Query(value = "DELETE FROM image WHERE member_id = :id", nativeQuery = true)
    int hardDeleteImagesOf(@Param("id") Long id);

    @Modifying @Query(value = "DELETE FROM notification WHERE receiver_id = :id OR sender_id = :id", nativeQuery = true)
    int hardDeleteNotificationsOf(@Param("id") Long id);

    /**
     * 위 정리 쿼리 실행 후 호출. payment, matching, project, chat 등
     * 비즈니스 레코드가 아직 참조하고 있으면 DataIntegrityViolationException 발생 → 호출자가 skip.
     */
    @Modifying @Query(value = "DELETE FROM member WHERE member_id = :id AND used = 'DELETED'", nativeQuery = true)
    int hardDeleteMemberById(@Param("id") Long id);
}
