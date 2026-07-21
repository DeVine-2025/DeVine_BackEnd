package com.umc.devine.domain.payment.repository.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.devine.domain.member.entity.QMember;
import com.umc.devine.domain.payment.entity.QPayment;
import com.umc.devine.domain.payment.entity.QTransaction;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.projection.AdminPaymentSummary;
import com.umc.devine.domain.ticket.entity.QPaymentTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaymentQueryDslImpl implements PaymentQueryDsl {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AdminPaymentSummary> searchForAdmin(
            String memberNickname,
            Long ticketProductId,
            LocalDateTime paidFrom,
            LocalDateTime paidUntil,
            Pageable pageable
    ) {
        QPayment payment = QPayment.payment;
        QTransaction transaction = QTransaction.transaction;
        QMember member = QMember.member;

        BooleanBuilder where = new BooleanBuilder(transaction.type.eq(TransactionType.PAYMENT));
        if (memberNickname != null && !memberNickname.isBlank()) {
            where.and(payment.member.nickname.eq(memberNickname));
        }
        if (paidFrom != null) {
            where.and(transaction.paidAt.goe(paidFrom));
        }
        if (paidUntil != null) {
            where.and(transaction.paidAt.lt(paidUntil));
        }
        if (ticketProductId != null) {
            // 조인 대신 exists로 걸어 행 중복 없이 필터한다.
            QPaymentTicket paymentTicket = QPaymentTicket.paymentTicket;
            where.and(JPAExpressions.selectOne()
                    .from(paymentTicket)
                    .where(paymentTicket.payment.eq(payment)
                            .and(paymentTicket.ticketProduct.id.eq(ticketProductId)))
                    .exists());
        }

        Long total = queryFactory
                .select(payment.count())
                .from(payment)
                .join(transaction).on(transaction.payment.eq(payment))
                .where(where)
                .fetchOne();

        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<AdminPaymentSummary> content = queryFactory
                .select(Projections.constructor(AdminPaymentSummary.class,
                        payment.id,
                        member.id,
                        member.nickname,
                        payment.orderName,
                        payment.amount,
                        transaction.paidAt,
                        transaction.status))
                .from(payment)
                .join(payment.member, member)
                .join(transaction).on(transaction.payment.eq(payment))
                .where(where)
                .orderBy(transaction.paidAt.desc(), payment.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }
}
