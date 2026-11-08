package com.umc.devine.domain.complaint.repository;

import com.umc.devine.domain.complaint.entity.ComplaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {

    List<ComplaintHistory> findByComplaintIdOrderByCreatedAtDesc(Long complaintId);
}
