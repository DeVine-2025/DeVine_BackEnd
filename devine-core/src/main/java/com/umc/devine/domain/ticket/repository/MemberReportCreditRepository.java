package com.umc.devine.domain.ticket.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberReportCreditRepository extends JpaRepository<MemberReportCredit, Long> {

    Optional<MemberReportCredit> findByMember(Member member);

    /** 크레딧 회수처럼 읽고-계산해서-쓰는 경로용. 동시 사용으로 인한 lost update를 막는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM MemberReportCredit c WHERE c.member = :member")
    Optional<MemberReportCredit> findByMemberForUpdate(@Param("member") Member member);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberReportCredit c SET c.remainingCount = c.remainingCount + :amount WHERE c.member = :member")
    int addCreditsByMember(@Param("member") Member member, @Param("amount") int amount);
}
