package com.umc.devine.domain.ticket.repository;

import com.umc.devine.domain.ticket.entity.TicketProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketProductRepository extends JpaRepository<TicketProduct, Long> {

    List<TicketProduct> findAllByActiveTrue();
}
