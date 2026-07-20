package com.umc.devine.admin.complaint.service.command;

import com.umc.devine.admin.complaint.converter.ComplaintConverter;
import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.entity.ComplaintHistory;
import com.umc.devine.admin.complaint.enums.ComplaintAction;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.exception.ComplaintException;
import com.umc.devine.admin.complaint.exception.code.ComplaintErrorReason;
import com.umc.devine.admin.complaint.repository.ComplaintHistoryRepository;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintCommandServiceImpl implements ComplaintCommandService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;
    private final MemberRepository memberRepository;

    @Override
    public ComplaintResDTO.UpdateStatusRes updateStatus(Long complaintId, Long processorMemberId, ComplaintReqDTO.UpdateStatusReq request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintException(ComplaintErrorReason.COMPLAINT_NOT_FOUND));

        boolean reprocessWarning = complaint.getStatus() == ComplaintStatus.COMPLETED;

        ComplaintAction action = request.action();
        String resolutionReason = request.reason();
        LocalDateTime resolvedAt = complaint.getResolvedAt();

        if (request.status() == ComplaintStatus.COMPLETED) {
            if (action == null) {
                throw new ComplaintException(ComplaintErrorReason.ACTION_REQUIRED);
            }
            if (!StringUtils.hasText(resolutionReason)) {
                throw new ComplaintException(ComplaintErrorReason.RESOLUTION_REASON_REQUIRED);
            }
            resolvedAt = LocalDateTime.now();

            // TODO: action == SUSPEND 인 경우 [계정 정지/정지해제/강제탈퇴] 기능 호출 필요 (피신고자 대상). 기능 미구현으로 현재는 신고 상태만 변경.
            // TODO: action == DELETE && targetType == PROJECT 인 경우 [프로젝트 게시글 노출/비노출 처리] 기능 호출 필요. 기능 미구현으로 현재는 신고 상태만 변경.
        }

        Member resolver = processorMemberId != null ? memberRepository.findById(processorMemberId).orElse(null) : null;

        complaint.updateStatus(request.status(), action, resolutionReason, resolver, resolvedAt);

        complaintHistoryRepository.save(ComplaintHistory.builder()
                .complaint(complaint)
                .status(request.status())
                .action(action)
                .resolutionReason(resolutionReason)
                .resolver(resolver)
                .build());

        return ComplaintConverter.toUpdateStatusRes(complaint, reprocessWarning);
    }
}
