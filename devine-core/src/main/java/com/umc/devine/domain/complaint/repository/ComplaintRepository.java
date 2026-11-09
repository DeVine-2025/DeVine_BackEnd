package com.umc.devine.domain.complaint.repository;

import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintAction;
import com.umc.devine.domain.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.complaint.repository.querydsl.ComplaintQueryDsl;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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
