package com.umc.devine.domain.ticket.repository;

import com.umc.devine.domain.ticket.entity.PaymentTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTicketRepository extends JpaRepository<PaymentTicket, Long> {
}
