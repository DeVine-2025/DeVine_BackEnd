package com.umc.devine.domain.payment.repository;

import com.umc.devine.domain.payment.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
}
