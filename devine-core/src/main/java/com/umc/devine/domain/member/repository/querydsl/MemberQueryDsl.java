package com.umc.devine.domain.member.repository.querydsl;

import com.querydsl.core.types.Predicate;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberQueryDsl {

    Page<Member> search(Predicate predicate, Pageable pageable);
}
