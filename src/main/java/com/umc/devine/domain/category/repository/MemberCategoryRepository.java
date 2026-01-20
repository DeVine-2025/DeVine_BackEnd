package com.umc.devine.domain.category.repository;

import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberCategoryRepository extends JpaRepository<MemberCategory, Long> {
    List<MemberCategory> findAllByMember(Member member);
}
