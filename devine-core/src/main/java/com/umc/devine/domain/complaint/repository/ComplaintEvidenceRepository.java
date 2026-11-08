package com.umc.devine.domain.complaint.repository;

import com.umc.devine.domain.complaint.entity.ComplaintEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplaintEvidenceRepository extends JpaRepository<ComplaintEvidence, Long> {

    Optional<ComplaintEvidence> findByComplaintId(Long complaintId);
}
