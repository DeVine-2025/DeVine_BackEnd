package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberLoginHistoryRepository extends JpaRepository<MemberLoginHistory, Long> {

    List<MemberLoginHistory> findTop10ByMemberIdOrderByLoginAtDesc(Long memberId);

    boolean existsByMemberIdAndLoginAtAfter(Long memberId, LocalDateTime threshold);

    /** Hard Delete 배치에서 회원 행을 지우기 전에 호출한다. member_id가 NOT NULL FK라 남겨두면 하드삭제가 막힌다. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberLoginHistory h WHERE h.member = :member")
    int bulkDeleteByMember(@Param("member") Member member);

    /** 통신비밀보호법에 따른 독립적인 3개월 보관 정책. 탈퇴 여부와 무관하게 모든 회원에게 적용된다. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberLoginHistory h WHERE h.loginAt < :threshold")
    int bulkDeleteByLoginAtBefore(@Param("threshold") LocalDateTime threshold);
}
