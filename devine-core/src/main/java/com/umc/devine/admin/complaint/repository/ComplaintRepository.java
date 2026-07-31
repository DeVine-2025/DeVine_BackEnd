package com.umc.devine.admin.complaint.repository;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.repository.querydsl.ComplaintQueryDsl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, ComplaintQueryDsl {

    long countByRespondentMemberId(Long respondentMemberId);

    List<Complaint> findByRespondentMemberIdOrderByCreatedAtDesc(Long respondentMemberId);
}
