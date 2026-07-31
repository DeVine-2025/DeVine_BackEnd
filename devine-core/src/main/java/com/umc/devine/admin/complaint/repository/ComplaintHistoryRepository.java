package com.umc.devine.admin.complaint.repository;

import com.umc.devine.admin.complaint.entity.ComplaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {

    List<ComplaintHistory> findByComplaintIdOrderByCreatedAtDesc(Long complaintId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ComplaintHistory h WHERE h.complaint.id IN :complaintIds")
    int bulkDeleteByComplaintIdIn(@Param("complaintIds") List<Long> complaintIds);
}
