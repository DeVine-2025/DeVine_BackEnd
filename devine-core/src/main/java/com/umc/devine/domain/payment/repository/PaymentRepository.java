package com.umc.devine.domain.payment.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPortonePaymentId(String portonePaymentId);

    @Query("SELECT DISTINCT p FROM Payment p LEFT JOIN FETCH p.transactions t LEFT JOIN FETCH t.cardDetail LEFT JOIN FETCH t.easyPayDetail WHERE p.member = :member ORDER BY p.createdAt DESC")
    List<Payment> findAllByMemberWithTransactions(@Param("member") Member member);
}
