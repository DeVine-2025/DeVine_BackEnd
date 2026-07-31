package com.umc.devine.admin.complaint.service.command;

import com.umc.devine.admin.complaint.converter.ComplaintConverter;
import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.entity.ComplaintHistory;
import com.umc.devine.admin.complaint.enums.ComplaintAction;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.admin.complaint.exception.ComplaintException;
import com.umc.devine.admin.complaint.exception.code.ComplaintErrorReason;
import com.umc.devine.admin.complaint.repository.ComplaintHistoryRepository;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.service.command.AdminMemberCommandService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.enums.MemberStatusAction;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.ProjectRepository;
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
    private final ProjectRepository projectRepository;
    private final AdminMemberCommandService adminMemberCommandService;

    @Override
    public ComplaintResDTO.UpdateStatusRes updateStatus(Long complaintId, String processorClerkId, ComplaintReqDTO.UpdateStatusReq request) {
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
        }

        Member resolver = processorClerkId != null ? memberRepository.findByClerkId(processorClerkId).orElse(null) : null;

        if (request.status() == ComplaintStatus.COMPLETED) {
            if (action == ComplaintAction.SUSPEND) {
                suspendRespondent(complaint, resolver, resolutionReason);
            }
            if (action == ComplaintAction.DELETE && complaint.getTargetType() == ComplaintTargetType.PROJECT) {
                hideReportedProject(complaint.getTargetId());
            }
        }

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

    private void hideReportedProject(Long projectId) {
        projectRepository.findById(projectId)
                .filter(project -> !ProjectStatus.INVISIBLE_STATUSES.contains(project.getStatus()))
                .ifPresent(Project::hide);
    }

    private void suspendRespondent(Complaint complaint, Member resolver, String resolutionReason) {
        Member respondent = complaint.getRespondentMember();
        // 이미 정지/탈퇴 등 최종 상태면 멱등 처리(DELETE 액션의 INVISIBLE_STATUSES 필터와 동일한 취지).
        // 그렇지 않으면 같은 상습 위반자를 두 건째 신고에서 SUSPEND 처리할 때 회원 도메인 예외가
        // 신고 트랜잭션 전체를 롤백시켜, 신고 상태 변경 자체가 실패한다.
        if (respondent.getUsed() != MemberStatus.ACTIVE && respondent.getUsed() != MemberStatus.INACTIVE) {
            return;
        }
        adminMemberCommandService.changeStatus(
                respondent.getNickname(),
                resolver,
                AdminMemberReqDTO.ChangeStatusReq.builder()
                        .action(MemberStatusAction.SUSPEND)
                        .reason(resolutionReason)
                        // 신고로 인한 정지는 항상 통지한다 — 관리자가 선택하는 직접 정지 API와 달리
                        // 신고 처리 결과를 당사자에게 알리지 않을 이유가 없어 의도적으로 고정했다.
                        .notifyRequested(Boolean.TRUE)
                        .build()
        );
    }
}
