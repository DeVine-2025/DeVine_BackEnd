package com.umc.devine.admin.complaint.service.command;

import com.umc.devine.admin.complaint.converter.ComplaintConverter;
import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.admin.member.service.command.AdminMemberCommandService;
import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.entity.ComplaintHistory;
import com.umc.devine.domain.complaint.enums.ComplaintAction;
import com.umc.devine.domain.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.complaint.exception.ComplaintException;
import com.umc.devine.domain.complaint.exception.code.ComplaintErrorReason;
import com.umc.devine.domain.complaint.helper.ComplaintNotificationHelper;
import com.umc.devine.domain.complaint.helper.ComplaintTargetReader;
import com.umc.devine.domain.complaint.repository.ComplaintHistoryRepository;
import com.umc.devine.domain.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.complaint.service.command.ComplaintModerationService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminComplaintCommandServiceImpl implements AdminComplaintCommandService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;
    private final MemberRepository memberRepository;
    private final ComplaintModerationService moderationService;
    private final ComplaintNotificationHelper notificationHelper;
    private final ComplaintTargetReader targetReader;
    private final AdminMemberCommandService adminMemberCommandService;

    @Override
    public ComplaintResDTO.UpdateStatusRes updateStatus(Long complaintId, String processorClerkId, ComplaintReqDTO.UpdateStatusReq request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintException(ComplaintErrorReason.COMPLAINT_NOT_FOUND));

        boolean reprocessWarning = complaint.getStatus() == ComplaintStatus.COMPLETED;

        ComplaintAction action = request.action();
        LocalDateTime resolvedAt = complaint.getResolvedAt();

        if (request.status() == ComplaintStatus.COMPLETED) {
            validateCompletion(complaint, request);
            resolvedAt = LocalDateTime.now();
        }

        Member resolver = processorClerkId != null ? memberRepository.findByClerkId(processorClerkId).orElse(null) : null;

        complaint.updateStatus(request.status(), action, request.reason(), request.notifySummary(), resolver, resolvedAt);

        complaintHistoryRepository.save(ComplaintHistory.builder()
                .complaint(complaint)
                .status(request.status())
                .action(action)
                .resolutionReason(request.reason())
                .resolver(resolver)
                .build());

        if (request.status() == ComplaintStatus.COMPLETED) {
            moderationService.applyResolution(complaint);
            suspendRespondentIfNeeded(complaint, processorClerkId);

            String targetName = targetReader.displayName(complaint);
            notificationHelper.notifyComplainant(complaint, targetName);
            notificationHelper.notifyRespondent(complaint, targetName);
        }

        return ComplaintConverter.toUpdateStatusRes(complaint, reprocessWarning);
    }

    private void validateCompletion(Complaint complaint, ComplaintReqDTO.UpdateStatusReq request) {
        if (request.action() == null) {
            throw new ComplaintException(ComplaintErrorReason.ACTION_REQUIRED);
        }
        if (!StringUtils.hasText(request.reason())) {
            throw new ComplaintException(ComplaintErrorReason.RESOLUTION_REASON_REQUIRED);
        }
        if (!StringUtils.hasText(request.notifySummary())) {
            throw new ComplaintException(ComplaintErrorReason.NOTIFY_SUMMARY_REQUIRED);
        }
        if (complaint.getTargetType() == ComplaintTargetType.DEVELOPER && request.action().isContentOnly()) {
            throw new ComplaintException(ComplaintErrorReason.ACTION_NOT_ALLOWED_FOR_TARGET);
        }
    }

     // 최종 상태 회원은 건너뛴다. 전이 예외가 나가면 같은 트랜잭션의 신고 상태 변경까지 롤백된다.
    private void suspendRespondentIfNeeded(Complaint complaint, String processorClerkId) {
        if (complaint.getAction() != ComplaintAction.SUSPEND) {
            return;
        }

        Member respondent = complaint.getRespondentMember();
        MemberStatus status = respondent.getUsed();
        if (status != MemberStatus.ACTIVE && status != MemberStatus.INACTIVE) {
            return;
        }

        adminMemberCommandService.changeStatus(respondent.getNickname(), processorClerkId,
                AdminMemberReqDTO.ChangeStatusReq.builder()
                        .action(MemberStatusAction.SUSPEND)
                        .reason(complaint.getNotifySummary())
                        .notifyRequested(true)
                        .build());

        complaint.markLinkedActionCompleted(LocalDateTime.now());
    }
}
