package com.umc.devine.admin.member.repository;

import com.umc.devine.admin.member.entity.MemberStatusHistory;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.umc.devine.domain.member.entity.Member;

public interface MemberStatusHistoryRepository extends JpaRepository<MemberStatusHistory, Long> {

    /** 보관 만료 파기 배치용: 회원별 마지막 액션 시각(탈퇴일 = 자진 탈퇴 행 시각). [memberId, createdAt] */
    @Query("SELECT h.member.id, MAX(h.createdAt) FROM MemberStatusHistory h WHERE h.action = :action GROUP BY h.member.id")
    List<Object[]> findLatestActionTimeByMember(@Param("action") MemberStatusAction action);

    /** 보관 만료 파기 배치용: 결정 C로 남겨 둔 기존 이력을 지운다. 자진 탈퇴 행은 탈퇴일 기준이라 남긴다. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM MemberStatusHistory h WHERE h.member = :member AND h.action <> :keep")
    int deleteAllByMemberExceptAction(@Param("member") Member member, @Param("keep") MemberStatusAction keep);

    /** 회원 탈퇴용: 이 회원의 기존 상태 변경 이력. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM MemberStatusHistory h WHERE h.member = :member")
    int deleteAllByMemberBulk(@Param("member") Member member);

    /** 회원 탈퇴용: 이 회원이 처리자로 남은 이력의 처리자 칸을 비운다(관리자도 회원이다). */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE MemberStatusHistory h SET h.processor = NULL WHERE h.processor = :member")
    int clearProcessor(@Param("member") Member member);

    List<MemberStatusHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
