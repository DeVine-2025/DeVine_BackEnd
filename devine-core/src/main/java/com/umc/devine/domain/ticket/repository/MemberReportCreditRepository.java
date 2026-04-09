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

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberReportCredit c SET c.remainingCount = c.remainingCount + :amount WHERE c.member = :member")
    int addCreditsByMember(@Param("member") Member member, @Param("amount") int amount);

    /**
     * 비관적 락 조회 — createReportSync의 동시성 제어용.
     * SELECT FOR UPDATE로 동일 회원의 동시 요청을 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM MemberReportCredit c WHERE c.member = :member")
    Optional<MemberReportCredit> findByMemberForUpdate(@Param("member") Member member);

    /**
     * 원자적 차감 — createReport(비동기)의 동시성 제어용.
     * remaining_count > 0 조건으로 크레딧 부족 시 0건 업데이트.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberReportCredit c SET c.remainingCount = c.remainingCount - 1 WHERE c.member = :member AND c.remainingCount > 0")
    int useCreditByMember(@Param("member") Member member);
}
