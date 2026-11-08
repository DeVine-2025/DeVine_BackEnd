package com.umc.devine.admin.complaint.converter;

import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.member.enums.MemberStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ComplaintConverter {

    public static ComplaintResDTO.ComplaintSummaryDTO toComplaintSummaryDTO(Complaint complaint, boolean slaExceeded) {
        return ComplaintResDTO.ComplaintSummaryDTO.builder()
                .complaintId(complaint.getId())
                .targetType(complaint.getTargetType())
                .complainantNickname(complaint.getComplainant().getNickname())
                .respondentNickname(complaint.getRespondentMember().getNickname())
                .reasonCode(complaint.getReasonCode())
                .createdAt(complaint.getCreatedAt())
                .status(complaint.getStatus())
                .action(complaint.getAction())
                .respondentWithdrawn(isWithdrawn(complaint))
                .slaExceeded(slaExceeded)
                .build();
    }

    public static boolean isSlaExceeded(Complaint complaint, LocalDateTime now) {
        return complaint.getStatus() != ComplaintStatus.COMPLETED
                && complaint.getCreatedAt().plusHours(48).isBefore(now);
    }

    public static ComplaintResDTO.ComplaintDetailRes toComplaintDetailRes(
            Complaint complaint,
            String content,
            long respondentComplaintCount,
            List<ComplaintResDTO.ComplaintSummaryDTO> respondentHistory
    ) {
        return ComplaintResDTO.ComplaintDetailRes.builder()
                .complaintId(complaint.getId())
                .targetType(complaint.getTargetType())
                .targetId(complaint.getTargetId())
                .complainantNickname(complaint.getComplainant().getNickname())
                .respondentNickname(complaint.getRespondentMember().getNickname())
                .reasonCode(complaint.getReasonCode())
                .detail(complaint.getDetail())
                .status(complaint.getStatus())
                .action(complaint.getAction())
                .resolutionReason(complaint.getResolutionReason())
                .notifySummary(complaint.getNotifySummary())
                .respondentWithdrawn(isWithdrawn(complaint))
                .createdAt(complaint.getCreatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .linkedActionCompleted(complaint.isLinkedActionCompleted())
                .content(content)
                .respondentComplaintCount(respondentComplaintCount)
                .respondentHistory(respondentHistory)
                .build();
    }

    public static ComplaintResDTO.UpdateStatusRes toUpdateStatusRes(Complaint complaint, boolean reprocessWarning) {
        return ComplaintResDTO.UpdateStatusRes.builder()
                .complaintId(complaint.getId())
                .status(complaint.getStatus())
                .action(complaint.getAction())
                .resolutionReason(complaint.getResolutionReason())
                .resolvedAt(complaint.getResolvedAt())
                .reprocessWarning(reprocessWarning)
                .linkedActionCompleted(complaint.isLinkedActionCompleted())
                .build();
    }

    private static boolean isWithdrawn(Complaint complaint) {
        return complaint.getRespondentMember().getUsed() == MemberStatus.DELETED;
    }
}
