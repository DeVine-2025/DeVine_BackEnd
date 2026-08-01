package com.umc.devine.domain.member.repository.querydsl;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.QContact;
import com.umc.devine.domain.member.entity.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberQueryDslImpl implements MemberQueryDsl {

    private final JPAQueryFactory queryFactory;

    /**
     * 다른 MemberRepository 조회 메서드와 달리 used = 'ACTIVE' 필터를 걸지 않는다.
     * 관리자는 정지/강제탈퇴예정 회원도 검색해서 조치할 수 있어야 하기 때문에 의도적으로 뺐다.
     */
    @Override
    public Page<Member> search(Predicate predicate, Pageable pageable) {
        QMember member = QMember.member;
        QContact contact = QContact.contact;

        List<Long> ids = queryFactory
                .select(member.id, member.createdAt)
                .distinct()
                .from(member)
                .leftJoin(contact).on(contact.member.eq(member))
                .where(predicate)
                .orderBy(member.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(tuple -> tuple.get(member.id))
                .toList();

        long total = queryFactory
                .select(member.countDistinct())
                .from(member)
                .leftJoin(contact).on(contact.member.eq(member))
                .where(predicate)
                .fetchOne();

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<Member> content = queryFactory
                .selectFrom(member)
                .where(member.id.in(ids))
                .orderBy(member.createdAt.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }
}
