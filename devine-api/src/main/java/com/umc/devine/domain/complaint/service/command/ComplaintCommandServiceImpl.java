package com.umc.devine.domain.complaint.service.command;

import com.umc.devine.domain.member.repository.MemberBlockRepository;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.complaint.dto.ComplaintReqDTO;
import com.umc.devine.domain.complaint.dto.ComplaintResDTO;
import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.entity.ComplaintEvidence;
import com.umc.devine.domain.complaint.enums.ComplaintReasonCode;
import com.umc.devine.domain.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.complaint.exception.ComplaintException;
import com.umc.devine.domain.complaint.exception.code.ComplaintErrorReason;
import com.umc.devine.domain.complaint.helper.ComplaintTargetReader;
import com.umc.devine.domain.complaint.repository.ComplaintEvidenceRepository;
import com.umc.devine.domain.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.infrastructure.email.EmailNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintCommandServiceImpl implements ComplaintCommandService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DAILY_LIMIT = 5;
    private static final String OPERATIONS_EMAIL = "projectdevine2025@gmail.com";
    private static final List<ComplaintStatus> IN_PROGRESS_STATUSES =
            List.of(ComplaintStatus.PENDING, ComplaintStatus.IN_REVIEW);

    private final MemberBlockRepository blockRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintEvidenceRepository complaintEvidenceRepository;
    private final MemberRepository memberRepository;
    private final ComplaintTargetReader targetReader;
    private final ComplaintModerationService moderationService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${complaint.evidence.enabled:false}")
    private boolean evidenceEnabled;

    private record Target(Member respondent, Long targetId, String displayName, String blockTargetNickname) {}

    @Override
    public ComplaintResDTO.CreateRes create(Member complainant, ComplaintReqDTO.CreateReq request) {
        validateDetail(request);

        Target target = resolveTarget(complainant, request);

        validateNotDuplicated(complainant, request.targetType(), target);
        validateDailyLimit(complainant);

        Complaint complaint = complaintRepository.save(Complaint.builder()
                .complainant(complainant)
                .respondentMember(target.respondent())
                .targetType(request.targetType())
                .targetId(target.targetId())
                .reasonCode(request.reasonCode())
                .detail(request.detail())
                .build());

        if (evidenceEnabled) {
            complaintEvidenceRepository.save(ComplaintEvidence.builder()
                    .complaint(complaint)
                    .content(targetReader.buildEvidence(request.targetType(), target.targetId(), target.respondent()))
                    .build());
        }

        moderationService.blindOnCreate(complaint);
        notifyOperations(complaint);

        return ComplaintResDTO.CreateRes.builder()
                .complaintId(complaint.getId())
                .targetType(complaint.getTargetType())
                .targetName(target.displayName())
                .blockTargetNickname(target.blockTargetNickname())
                .blockTargetAlreadyBlocked(blockRepository.existsByBlockerIdAndTargetId(complainant.getId(), target.respondent().getId()))
                .build();
    }

    private void validateDetail(ComplaintReqDTO.CreateReq request) {
        if (request.reasonCode() == ComplaintReasonCode.ETC && !StringUtils.hasText(request.detail())) {
            throw new ComplaintException(ComplaintErrorReason.DETAIL_REQUIRED);
        }
    }

    private Target resolveTarget(Member complainant, ComplaintReqDTO.CreateReq request) {
        return switch (request.targetType()) {
            case PROJECT -> resolveProject(complainant, request.targetId());
            case DEVELOPER -> resolveDeveloper(complainant, request.targetNickname());
            case CHAT -> resolveChatMessage(complainant, request.targetId());
        };
    }

    private Target resolveProject(Member complainant, Long projectId) {
        if (projectId == null) {
            throw new ComplaintException(ComplaintErrorReason.TARGET_NOT_FOUND);
        }
        Project project = targetReader.findVisibleProjectWithMember(projectId)
                .orElseThrow(() -> new ComplaintException(ComplaintErrorReason.TARGET_NOT_FOUND));
        if (project.isOwnedBy(complainant)) {
            throw new ComplaintException(ComplaintErrorReason.SELF_COMPLAINT);
        }
        Member pm = project.getMember();
        if (blockRepository.existsBetween(complainant.getId(), pm.getId())) {
            throw new ComplaintException(ComplaintErrorReason.TARGET_NOT_FOUND);
        }
        return new Target(pm, project.getId(), project.getName(), pm.getNickname());
    }

    private Target resolveDeveloper(Member complainant, String nickname) {
        if (!StringUtils.hasText(nickname)) {
            throw new ComplaintException(ComplaintErrorReason.TARGET_NOT_FOUND);
        }
        Member respondent = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new ComplaintException(ComplaintErrorReason.TARGET_NOT_FOUND));
        if (respondent.getId().equals(complainant.getId())) {
            throw new ComplaintException(ComplaintErrorReason.SELF_COMPLAINT);
        }
        if (blockRepository.existsBetween(complainant.getId(), respondent.getId())) {
            throw new ComplaintException(ComplaintErrorReason.TARGET_NOT_FOUND);
        }
        return new Target(respondent, null, respondent.getNickname(), respondent.getNickname());
    }

    private Target resolveChatMessage(Member complainant, Long chatMessageId) {
        ChatMessage message = targetReader.findChatMessage(chatMessageId)
                .orElseThrow(() -> new ComplaintException(ComplaintErrorReason.TARGET_NOT_FOUND));

        ChatRoom room = message.getChatRoom();
        boolean participant = room.getMember1().getId().equals(complainant.getId())
                || room.getMember2().getId().equals(complainant.getId());
        if (!participant) {
            throw new ComplaintException(ComplaintErrorReason.TARGET_NOT_FOUND);
        }

        Member sender = message.getSender();
        if (sender.getId().equals(complainant.getId())) {
            throw new ComplaintException(ComplaintErrorReason.SELF_COMPLAINT);
        }
        return new Target(sender, message.getId(), ComplaintTargetReader.CHAT_MESSAGE_NAME, sender.getNickname());
    }

    private void validateNotDuplicated(Member complainant, ComplaintTargetType targetType, Target target) {
        if (targetType == ComplaintTargetType.DEVELOPER) {
            boolean inProgress = complaintRepository
                    .existsByComplainantIdAndRespondentMemberIdAndTargetTypeAndStatusIn(
                            complainant.getId(), target.respondent().getId(), targetType, IN_PROGRESS_STATUSES);
            if (inProgress) {
                throw new ComplaintException(ComplaintErrorReason.DUPLICATE_RESPONDENT);
            }
            return;
        }

        if (complaintRepository.existsByComplainantIdAndTargetTypeAndTargetId(
                complainant.getId(), targetType, target.targetId())) {
            throw new ComplaintException(ComplaintErrorReason.DUPLICATE_TARGET);
        }
    }

    private void validateDailyLimit(Member complainant) {
        LocalDate today = LocalDate.now(KST);
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay().minusNanos(1);

        if (complaintRepository.countByComplainantIdAndCreatedAtBetween(complainant.getId(), from, to) >= DAILY_LIMIT) {
            throw new ComplaintException(ComplaintErrorReason.DAILY_LIMIT_EXCEEDED);
        }
    }

    private void notifyOperations(Complaint complaint) {
        eventPublisher.publishEvent(EmailNotificationEvent.builder()
                .to(OPERATIONS_EMAIL)
                .subject("[DeVine] 새 신고가 접수됐습니다 (신고 번호 %d)".formatted(complaint.getId()))
                .body("신고 번호: %d<br />대상 종류: %s".formatted(complaint.getId(), complaint.getTargetType()))
                .build());
    }
}
