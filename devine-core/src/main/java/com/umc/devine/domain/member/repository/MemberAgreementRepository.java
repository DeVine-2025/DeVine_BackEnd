package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberAgreement;
import com.umc.devine.domain.member.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Long> {

    /** 회원 탈퇴용. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM MemberAgreement ma WHERE ma.member = :member")
    int deleteAllByMemberBulk(@Param("member") Member member);

    List<MemberAgreement> findAllByMember(Member member);

    Optional<MemberAgreement> findByMemberAndTerms(Member member, Terms terms);

    boolean existsByMemberAndTerms(Member member, Terms terms);
}
