package com.umc.devine.domain.payment.entity;

import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "easy_pay_detail")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class EasyPayDetail extends BaseEntity {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private Transaction transaction;

    void assignTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Column
    private String provider;

    @Column(name = "card_name")
    private String cardName;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "approval_number")
    private String approvalNumber;

    @Column(name = "installment_month")
    private Integer installmentMonth;
}
