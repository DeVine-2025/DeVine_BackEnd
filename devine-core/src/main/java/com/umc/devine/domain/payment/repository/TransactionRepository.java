package com.umc.devine.domain.payment.repository;

import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Between은 양끝을 포함하므로, 날짜 경계가 중복 집계되지 않도록 [from, to) 반열린 구간으로 센다. */
    long countByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
            TransactionStatus status, LocalDateTime from, LocalDateTime to);
}
