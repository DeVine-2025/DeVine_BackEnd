package com.umc.devine.domain.complaint.repository;

import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintAction;
import com.umc.devine.domain.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.complaint.repository.querydsl.ComplaintQueryDsl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, ComplaintQueryDsl {

    long countByRespondentMemberId(Long respondentMemberId);

    long countByStatus(ComplaintStatus status);

    List<Complaint> findByRespondentMemberIdOrderByCreatedAtDesc(Long respondentMemberId);

    boolean existsByComplainantIdAndTargetTypeAndTargetId(
            Long complainantId, ComplaintTargetType targetType, Long targetId);

    boolean existsByComplainantIdAndRespondentMemberIdAndTargetTypeAndStatusIn(
            Long complainantId, Long respondentMemberId, ComplaintTargetType targetType,
            Collection<ComplaintStatus> statuses);

    long countByComplainantIdAndCreatedAtBetween(Long complainantId, LocalDateTime from, LocalDateTime to);

    long countByTargetTypeAndTargetIdAndStatusNot(
            ComplaintTargetType targetType, Long targetId, ComplaintStatus status);

    long countByTargetTypeAndTargetIdAndActionIn(
            ComplaintTargetType targetType, Long targetId, Collection<ComplaintAction> actions);

    List<Complaint> findByComplainantIdAndTargetTypeAndTargetIdIn(
            Long complainantId, ComplaintTargetType targetType, Collection<Long> targetIds);

    List<Complaint> findByTargetTypeAndTargetIdAndStatusNot(
            ComplaintTargetType targetType, Long targetId, ComplaintStatus status);
}
