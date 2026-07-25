package com.umc.devine.admin.complaint.repository;

import com.umc.devine.admin.complaint.entity.ComplaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {

    List<ComplaintHistory> findByComplaintIdOrderByCreatedAtDesc(Long complaintId);
}
