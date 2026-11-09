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

    @Query("SELECT h.member.id, MAX(h.createdAt) FROM MemberStatusHistory h WHERE h.action = :action GROUP BY h.member.id")
    List<Object[]> findLatestActionTimeByMember(@Param("action") MemberStatusAction action);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM MemberStatusHistory h WHERE h.member = :member AND h.action <> :keep")
    int deleteAllByMemberExceptAction(@Param("member") Member member, @Param("keep") MemberStatusAction keep);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM MemberStatusHistory h WHERE h.member = :member")
    int deleteAllByMemberBulk(@Param("member") Member member);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE MemberStatusHistory h SET h.processor = NULL WHERE h.processor = :member")
    int clearProcessor(@Param("member") Member member);

    List<MemberStatusHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
