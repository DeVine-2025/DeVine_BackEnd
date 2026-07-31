package com.umc.devine.admin.complaint.repository;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.repository.querydsl.ComplaintQueryDsl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, ComplaintQueryDsl {

    long countByRespondentMemberId(Long respondentMemberId);

    List<Complaint> findByRespondentMemberIdOrderByCreatedAtDesc(Long respondentMemberId);

    List<Complaint> findByComplainantId(Long complainantId);

    /** 회원 자진 탈퇴 시, 본인이 신고자(complainant)로서 접수한 신고만 삭제한다. 피신고자(respondent)로서 남은 이력은 보존한다. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Complaint c WHERE c.complainant.id = :complainantId")
    int bulkDeleteByComplainantId(@Param("complainantId") Long complainantId);
}
