package com.umc.devine.domain.complaint.repository;

import com.umc.devine.domain.complaint.entity.ComplaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.umc.devine.domain.member.entity.Member;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ComplaintHistory h SET h.resolver = NULL WHERE h.resolver = :member")
    int clearResolver(@Param("member") Member member);

    List<ComplaintHistory> findByComplaintIdOrderByCreatedAtDesc(Long complaintId);
}
