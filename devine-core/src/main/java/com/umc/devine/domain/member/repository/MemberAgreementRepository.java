package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberAgreement;
import com.umc.devine.domain.member.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Long> {

    List<MemberAgreement> findAllByMember(Member member);

    Optional<MemberAgreement> findByMemberAndTerms(Member member, Terms terms);

    boolean existsByMemberAndTerms(Member member, Terms terms);

    /** Hard Delete 배치에서 회원 행을 지우기 전에 호출한다. member_id가 NOT NULL FK라 남겨두면 하드삭제가 막힌다. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberAgreement a WHERE a.member = :member")
    int bulkDeleteByMember(@Param("member") Member member);
}
