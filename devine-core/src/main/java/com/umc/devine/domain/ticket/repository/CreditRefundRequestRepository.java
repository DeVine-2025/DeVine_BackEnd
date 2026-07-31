package com.umc.devine.domain.ticket.repository;

import com.umc.devine.domain.ticket.entity.CreditRefundRequest;
import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CreditRefundRequestRepository extends JpaRepository<CreditRefundRequest, Long> {

    Page<CreditRefundRequest> findAllByStatus(CreditRefundStatus status, Pageable pageable);

    /** 동일 건에 대한 동시 처리완료 요청이 서로 경쟁하지 않도록 행 잠금을 건다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CreditRefundRequest r WHERE r.id = :id")
    Optional<CreditRefundRequest> findByIdForUpdate(@Param("id") Long id);
}
