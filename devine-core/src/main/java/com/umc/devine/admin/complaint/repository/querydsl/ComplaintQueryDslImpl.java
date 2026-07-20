package com.umc.devine.admin.complaint.repository.querydsl;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.entity.QComplaint;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ComplaintQueryDslImpl implements ComplaintQueryDsl {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Complaint> search(Predicate predicate, Pageable pageable) {
        QComplaint complaint = QComplaint.complaint;

        long total = queryFactory
                .select(complaint.count())
                .from(complaint)
                .where(predicate)
                .fetchOne();

        List<Complaint> content = queryFactory
                .selectFrom(complaint)
                .leftJoin(complaint.complainant).fetchJoin()
                .leftJoin(complaint.respondentMember).fetchJoin()
                .where(predicate)
                .orderBy(complaint.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }
}
