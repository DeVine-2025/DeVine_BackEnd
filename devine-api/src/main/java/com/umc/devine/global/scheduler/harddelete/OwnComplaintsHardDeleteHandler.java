package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.repository.ComplaintHistoryRepository;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** 본인이 신고자(complainant)로서 접수한 신고만 삭제한다. 피신고자(respondent)로 남은 이력은 보존한다. */
@Component
@Order(30)
@RequiredArgsConstructor
public class OwnComplaintsHardDeleteHandler implements MemberHardDeleteHandler {

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;

    @Override
    public void handle(Member member) {
        List<Complaint> ownComplaints = complaintRepository.findByComplainantId(member.getId());
        if (ownComplaints.isEmpty()) {
            return;
        }
        List<Long> complaintIds = ownComplaints.stream().map(Complaint::getId).toList();
        complaintHistoryRepository.bulkDeleteByComplaintIdIn(complaintIds);
        complaintRepository.bulkDeleteByComplainantId(member.getId());
    }
}
