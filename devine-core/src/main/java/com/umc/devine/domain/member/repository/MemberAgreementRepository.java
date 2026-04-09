package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberAgreement;
import com.umc.devine.domain.member.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Long> {

    List<MemberAgreement> findAllByMember(Member member);

    Optional<MemberAgreement> findByMemberAndTerms(Member member, Terms terms);

    boolean existsByMemberAndTerms(Member member, Terms terms);
}
