package com.umc.devine.domain.payment.entity;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "portone_payment_id", unique = true, nullable = false)
    private String portonePaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "order_name", nullable = false)
    private String orderName;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 20)
    private String currency;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentTicket> paymentTickets = new ArrayList<>();

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
    }

    public void addPaymentTicket(PaymentTicket paymentTicket) {
        this.paymentTickets.add(paymentTicket);
    }
}
