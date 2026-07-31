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


    /** 중복 무시 삽입 — 이미 행이 있으면 아무 작업도 하지 않는다 (멱등) */
    @Modifying
    @Query(value = "INSERT INTO member_report_credit (member_id, remaining_count) VALUES (:memberId, :count) ON CONFLICT (member_id) DO NOTHING", nativeQuery = true)
    void insertIfNotExists(@Param("memberId") Long memberId, @Param("count") int count);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE MemberReportCredit c SET c.remainingCount = c.remainingCount + :amount WHERE c.member = :member")
    int addCreditsByMember(@Param("member") Member member, @Param("amount") int amount);

    /**
     * 원자적 차감 — createReport의 동시성 제어용.
     * remaining_count > 0 조건으로 크레딧 부족 시 0건 업데이트.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberReportCredit c SET c.remainingCount = c.remainingCount - 1 WHERE c.member = :member AND c.remainingCount > 0")
    int useCreditByMember(@Param("member") Member member);

    /** Hard Delete 배치에서 회원 행을 지우기 전에 호출한다. member_id가 NOT NULL FK라 남겨두면 하드삭제가 막힌다. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberReportCredit c WHERE c.member = :member")
    int bulkDeleteByMember(@Param("member") Member member);
}
