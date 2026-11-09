package com.umc.devine.admin.complaint.repository;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.repository.querydsl.ComplaintQueryDsl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.umc.devine.domain.member.entity.Member;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, ComplaintQueryDsl {

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Complaint c SET c.complainant = NULL WHERE c.complainant = :member")
    int clearComplainant(@Param("member") Member member);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Complaint c SET c.respondentMember = NULL WHERE c.respondentMember = :member")
    int clearRespondent(@Param("member") Member member);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Complaint c SET c.resolver = NULL WHERE c.resolver = :member")
    int clearResolver(@Param("member") Member member);

    long countByRespondentMemberId(Long respondentMemberId);

    long countByStatus(ComplaintStatus status);

    List<Complaint> findByRespondentMemberIdOrderByCreatedAtDesc(Long respondentMemberId);
}
