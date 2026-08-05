package com.umc.devine.admin.complaint.converter;

import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.admin.complaint.entity.Complaint;

import java.time.LocalDateTime;
import java.util.List;

public class ComplaintConverter {

    public static ComplaintResDTO.ComplaintSummaryDTO toComplaintSummaryDTO(Complaint complaint, boolean slaExceeded) {
        return ComplaintResDTO.ComplaintSummaryDTO.builder()
                .complaintId(complaint.getId())
                .targetType(complaint.getTargetType())
                .complainantNickname(complaint.getComplainant().getNickname())
                .respondentNickname(complaint.getRespondentMember().getNickname())
                .createdAt(complaint.getCreatedAt())
                .status(complaint.getStatus())
                .slaExceeded(slaExceeded)
                .build();
    }

    public static boolean isSlaExceeded(Complaint complaint, LocalDateTime now) {
        return complaint.getStatus() != com.umc.devine.admin.complaint.enums.ComplaintStatus.COMPLETED
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
                .reason(complaint.getReason())
                .status(complaint.getStatus())
                .action(complaint.getAction())
                .resolutionReason(complaint.getResolutionReason())
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
}
