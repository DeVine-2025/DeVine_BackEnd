package com.umc.devine.domain.category.repository;

import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberCategoryRepository extends JpaRepository<MemberCategory, Long> {
    List<MemberCategory> findAllByMember(Member member);
    void deleteAllByMember(Member member);

    @Query("SELECT mc FROM MemberCategory mc JOIN FETCH mc.category WHERE mc.member = :member")
    List<MemberCategory> findAllByMemberWithCategory(@Param("member") Member member);
}
