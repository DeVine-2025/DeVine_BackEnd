package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberStatusHistoryRepository extends JpaRepository<MemberStatusHistory, Long> {

    List<MemberStatusHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /** Hard Delete 배치에서 회원 행을 지우기 전에 호출한다. member_id가 NOT NULL FK라 남겨두면 하드삭제가 막힌다. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberStatusHistory h WHERE h.member = :member")
    int bulkDeleteByMember(@Param("member") Member member);

    /** 이 회원이 다른 회원의 상태변경을 처리한 이력(processor)이 있으면 하드삭제 전에 참조를 끊는다. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberStatusHistory h SET h.processor = null WHERE h.processor = :processor")
    int bulkNullifyProcessor(@Param("processor") Member processor);
}
