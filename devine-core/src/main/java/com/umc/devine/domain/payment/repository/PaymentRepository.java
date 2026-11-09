package com.umc.devine.domain.payment.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.repository.querydsl.PaymentQueryDsl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentQueryDsl {

    boolean existsByPortonePaymentId(String portonePaymentId);

    @Query("SELECT DISTINCT p FROM Payment p JOIN FETCH p.member LEFT JOIN FETCH p.transactions t LEFT JOIN FETCH t.cardDetail LEFT JOIN FETCH t.easyPayDetail WHERE p.id = :paymentId")
    Optional<Payment> findByIdWithMemberAndTransactions(@Param("paymentId") Long paymentId);

    @Query("SELECT DISTINCT p FROM Payment p LEFT JOIN FETCH p.transactions t LEFT JOIN FETCH t.cardDetail LEFT JOIN FETCH t.easyPayDetail WHERE p.member = :member ORDER BY p.createdAt DESC")
    List<Payment> findAllByMemberWithTransactions(@Param("member") Member member);

    /** 보관 만료 파기 배치용(01-1 FUNC-MEM-007 4번): 탈퇴 회원의 결제 중 결제일이 기준 시각 이전인 것. 환불 조건은 호출하는 쪽이 본다. */
    @Query("SELECT p.id FROM Payment p WHERE p.member.used = com.umc.devine.domain.member.enums.MemberStatus.DELETED " +
            "AND p.createdAt < :threshold")
    List<Long> findIdsOfWithdrawnMembersCreatedBefore(@Param("threshold") java.time.LocalDateTime threshold);
}
