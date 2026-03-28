package com.umc.devine.domain.ticket.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberReportCreditRepository extends JpaRepository<MemberReportCredit, Long> {

    Optional<MemberReportCredit> findByMember(Member member);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberReportCredit c SET c.remainingCount = c.remainingCount + :amount WHERE c.member = :member")
    int addCreditsByMember(@Param("member") Member member, @Param("amount") int amount);
}
