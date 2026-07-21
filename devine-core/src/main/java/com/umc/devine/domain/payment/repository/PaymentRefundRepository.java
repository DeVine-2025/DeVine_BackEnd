package com.umc.devine.domain.payment.repository;

import com.umc.devine.domain.payment.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {

    /** 환불 재시도로 한 결제에 여러 로우가 생길 수 있어 id 오름차순으로 반환한다. 뒤쪽이 최신. */
    @Query("SELECT r FROM PaymentRefund r WHERE r.payment.id IN :paymentIds ORDER BY r.id ASC")
    List<PaymentRefund> findAllByPaymentIdIn(@Param("paymentIds") Collection<Long> paymentIds);

    Optional<PaymentRefund> findTopByPaymentIdOrderByIdDesc(Long paymentId);
}
