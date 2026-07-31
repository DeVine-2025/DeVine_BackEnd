package com.umc.devine.domain.ticket.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.entity.CreditRefundRequest;
import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CreditRefundRequestRepository extends JpaRepository<CreditRefundRequest, Long> {

    Page<CreditRefundRequest> findAllByStatus(CreditRefundStatus status, Pageable pageable);

    /** 동일 건에 대한 동시 처리완료 요청이 서로 경쟁하지 않도록 행 잠금을 건다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CreditRefundRequest r WHERE r.id = :id")
    Optional<CreditRefundRequest> findByIdForUpdate(@Param("id") Long id);

    /**
     * Hard Delete 배치에서 호출한다. 미처리(REQUESTED) 건을 그냥 지우면 환불 청구권이 흔적 없이
     * 사라지므로, 행은 보존하고 EXPIRED로 전이한 뒤(관리자 미처리로 소멸했다는 감사 기록) member_id만
     * 끊는다(bulkDetachMember와 함께 사용). 이미 PROCESSED인 건은 상태를 건드리지 않는다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditRefundRequest r SET r.status = :expired, r.processedAt = CURRENT_TIMESTAMP "
            + "WHERE r.member = :member AND r.status = :requested")
    int bulkExpireUnprocessed(@Param("member") Member member,
                              @Param("requested") CreditRefundStatus requested,
                              @Param("expired") CreditRefundStatus expired);

    /** 하드삭제 대상 회원의 모든 환불 신청 행에서 member_id를 끊는다(REQUESTED/EXPIRED/PROCESSED 무관, 행은 보존). */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditRefundRequest r SET r.member = null WHERE r.member = :member")
    int bulkDetachMember(@Param("member") Member member);

    /** 이 회원이 다른 회원의 환불 신청을 처리한 이력(processor)이 있으면 하드삭제 전에 참조를 끊는다. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditRefundRequest r SET r.processor = null WHERE r.processor = :processor")
    int bulkNullifyProcessor(@Param("processor") Member processor);
}
