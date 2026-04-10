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

    /** 중복 무시 삽입 — 이미 행이 있으면 아무 작업도 하지 않는다 (멱등) */
    @Modifying
    @Query(value = "INSERT INTO member_report_credit (member_id, remaining_count) VALUES (:memberId, :count) ON CONFLICT (member_id) DO NOTHING", nativeQuery = true)
    void insertIfNotExists(@Param("memberId") Long memberId, @Param("count") int count);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberReportCredit c SET c.remainingCount = c.remainingCount + :amount WHERE c.member = :member")
    int addCreditsByMember(@Param("member") Member member, @Param("amount") int amount);

    /** 상한 초과 방지 환불용 — remainingCount + amount > maxCount이면 0건 업데이트 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberReportCredit c SET c.remainingCount = c.remainingCount + :amount WHERE c.member = :member AND c.remainingCount + :amount <= :maxCount")
    int addCreditsWithCap(@Param("member") Member member, @Param("amount") int amount, @Param("maxCount") int maxCount);

    /**
     * 비관적 락 조회 — createReportSync의 동시성 제어용.
     * SELECT FOR UPDATE로 동일 회원의 동시 요청을 직렬화한다.
     */
    @Deprecated // TODO : 비동기 전환 후 제거 예정
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
