package com.umc.devine.domain.ticket.repository;

import com.umc.devine.domain.ticket.entity.CreditRefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditRefundRequestRepository extends JpaRepository<CreditRefundRequest, Long> {
}
