package com.umc.devine.domain.ticket.repository;

import com.umc.devine.domain.ticket.entity.PaymentTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentTicketRepository extends JpaRepository<PaymentTicket, Long> {

    @Query("SELECT pt FROM PaymentTicket pt JOIN FETCH pt.ticketProduct WHERE pt.payment.id = :paymentId")
    List<PaymentTicket> findAllByPaymentIdWithProduct(@Param("paymentId") Long paymentId);
}
