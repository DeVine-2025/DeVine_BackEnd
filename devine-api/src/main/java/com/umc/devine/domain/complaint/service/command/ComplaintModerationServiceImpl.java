package com.umc.devine.domain.complaint.service.command;

import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintAction;
import com.umc.devine.domain.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.complaint.helper.ComplaintNotificationHelper;
import com.umc.devine.domain.complaint.helper.ComplaintTargetReader;
import com.umc.devine.domain.complaint.repository.ComplaintEvidenceRepository;
import com.umc.devine.domain.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.project.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintModerationServiceImpl implements ComplaintModerationService {

    private static final List<ComplaintAction> BLINDING_ACTIONS =
            List.of(ComplaintAction.DELETE, ComplaintAction.SUSPEND);

    private final ComplaintRepository complaintRepository;
    private final ComplaintEvidenceRepository complaintEvidenceRepository;
    private final ComplaintTargetReader targetReader;
    private final ComplaintNotificationHelper notificationHelper;

    @Override
    public void blindOnCreate(Complaint complaint) {
        if (complaint.getTargetType() != ComplaintTargetType.PROJECT) {
            return;
        }

        targetReader.findBlindableProject(complaint.getTargetId()).ifPresent(project -> {
            project.changeReportBlind(true, LocalDateTime.now());
            notificationHelper.notifyProjectBlinded(complaint, project.getMember(), project.getName());
        });
    }

    @Override
    public void applyResolution(Complaint complaint) {
        switch (complaint.getTargetType()) {
            case PROJECT -> applyProjectResolution(complaint);
            case CHAT -> applyChatResolution(complaint);
            case DEVELOPER -> {
            }
        }
    }

    private void applyProjectResolution(Complaint complaint) {
        Project project = targetReader.findBlindableProject(complaint.getTargetId()).orElse(null);
        if (project == null) {
            return;
        }

        if (keepsBlinded(complaint)) {
            project.changeReportBlind(true, LocalDateTime.now());
            complaint.markLinkedActionCompleted(LocalDateTime.now());
            return;
        }

        if (canRestore(complaint)) {
            project.changeReportBlind(false, LocalDateTime.now());
        }
    }

    private void applyChatResolution(Complaint complaint) {
        ChatMessage message = targetReader.findChatMessage(complaint.getTargetId()).orElse(null);
        if (message == null) {
            return;
        }

        if (keepsBlinded(complaint)) {
            if (!message.isHiddenByPolicy()) {
                message.hideByPolicy();
                complaint.markLinkedActionCompleted(LocalDateTime.now());
            }
            return;
        }

        if (message.isHiddenByPolicy() && canRestore(complaint)) {
            message.showByPolicy();
        }
    }

    private boolean canRestore(Complaint complaint) {
        long inProgress = complaintRepository.countByTargetTypeAndTargetIdAndStatusNot(
                complaint.getTargetType(), complaint.getTargetId(), ComplaintStatus.COMPLETED);
        long blindingResults = complaintRepository.countByTargetTypeAndTargetIdAndActionIn(
                complaint.getTargetType(), complaint.getTargetId(), BLINDING_ACTIONS);

        return inProgress == 0 && blindingResults == 0;
    }

    private boolean keepsBlinded(Complaint complaint) {
        return complaint.isCompleted() && complaint.getAction() != null && complaint.getAction().keepsBlinded();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwnerEditLocked(Long projectId) {
        List<Complaint> inProgress = complaintRepository.findByTargetTypeAndTargetIdAndStatusNot(
                ComplaintTargetType.PROJECT, projectId, ComplaintStatus.COMPLETED);

        return inProgress.stream()
                .anyMatch(complaint -> complaintEvidenceRepository.findByComplaintId(complaint.getId()).isEmpty());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConfirmedViolation(Long projectId) {
        return complaintRepository.countByTargetTypeAndTargetIdAndActionIn(
                ComplaintTargetType.PROJECT, projectId, BLINDING_ACTIONS) > 0;
    }
}
