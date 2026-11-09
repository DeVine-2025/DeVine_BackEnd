package com.umc.devine.admin.complaint.repository;

import com.umc.devine.admin.complaint.entity.ComplaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.umc.devine.domain.member.entity.Member;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {

    /** 회원 탈퇴용: 처리자 칸을 비운다. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE ComplaintHistory h SET h.resolver = NULL WHERE h.resolver = :member")
    int clearResolver(@Param("member") Member member);

    List<ComplaintHistory> findByComplaintIdOrderByCreatedAtDesc(Long complaintId);
}
