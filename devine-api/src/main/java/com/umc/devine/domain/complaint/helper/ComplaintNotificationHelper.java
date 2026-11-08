package com.umc.devine.domain.complaint.helper;

import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintAction;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.service.command.NotificationCommandService;
import com.umc.devine.infrastructure.email.EmailNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ComplaintNotificationHelper {

    private static final String SUPPORT_EMAIL = "projectdevine2025@gmail.com";
    private static final String APPEAL_GUIDE =
            "다른 의견이 있으면 7일 안에 신고 번호와 함께 " + SUPPORT_EMAIL + "으로 알려 주세요.";

    private final NotificationCommandService notificationCommandService;
    private final ContactRepository contactRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void notifyProjectBlinded(Complaint complaint, Member pm, String targetName) {
        notificationCommandService.create(
                NotificationType.COMPLAINT_PROJECT_BLINDED,
                pm.getId(),
                null,
                "'%s'에 신고가 접수돼 잠시 가렸어요. 검토 결과를 알려 드릴게요.".formatted(targetName),
                complaint.getId()
        );
    }

    public void notifyComplainant(Complaint complaint, String targetName) {
        Member receiver = complaint.getComplainant();
        if (isWithdrawn(receiver)) {
            return;
        }
        send(NotificationType.COMPLAINT_RESOLVED_COMPLAINANT, receiver, complaint,
                complainantMessage(complaint, targetName));
    }

    public void notifyRespondent(Complaint complaint, String targetName) {
        Member receiver = complaint.getRespondentMember();
        if (isWithdrawn(receiver) || complaint.getAction() == ComplaintAction.WITHDRAWN) {
            return;
        }
        send(NotificationType.COMPLAINT_RESOLVED_RESPONDENT, receiver, complaint,
                respondentMessage(complaint, targetName));
    }

    private String complainantMessage(Complaint complaint, String targetName) {
        String summary = reasonSummary(complaint);
        return switch (complaint.getAction()) {
            case DISMISS -> """
                    '%s' 신고를 검토했지만 운영 정책 위반을 확인하지 못했어요.
                    %s%s
                    (신고 번호 %d)""".formatted(targetName, summary, APPEAL_GUIDE, complaint.getId());
            case WITHDRAWN -> "신고하신 회원이 탈퇴해 신고가 종료됐어요. (신고 번호 %d)".formatted(complaint.getId());
            default -> """
                    '%s' 신고를 검토해 운영 정책에 따라 조치했어요. 신고해 주셔서 감사해요.
                    %s%s
                    (신고 번호 %d)""".formatted(targetName, summary, APPEAL_GUIDE, complaint.getId());
        };
    }

    private String respondentMessage(Complaint complaint, String targetName) {
        String summary = reasonSummary(complaint);
        return switch (complaint.getAction()) {
            case DISMISS -> """
                    '%s'에 접수된 신고를 검토한 결과 문제가 없어 종료했어요.
                    %s
                    (신고 번호 %d)""".formatted(targetName, APPEAL_GUIDE, complaint.getId());
            case WARNING -> """
                    '%s'이(가) 운영 정책을 위반한 것으로 확인돼 경고를 드려요.
                    %s%s
                    (신고 번호 %d)""".formatted(targetName, summary, APPEAL_GUIDE, complaint.getId());
            default -> """
                    '%s'이(가) 운영 정책을 위반한 것으로 확인돼 조치했어요.
                    %s%s
                    (신고 번호 %d)""".formatted(targetName, summary, APPEAL_GUIDE, complaint.getId());
        };
    }

    private String reasonSummary(Complaint complaint) {
        String summary = complaint.getNotifySummary();
        return summary == null || summary.isBlank() ? "" : "사유: %s%n".formatted(summary);
    }

    private void send(NotificationType type, Member receiver, Complaint complaint, String content) {
        notificationCommandService.create(type, receiver.getId(), null, content, complaint.getId());

        if (receiver.getUsed() == MemberStatus.SUSPENDED) {
            findEmail(receiver).ifPresent(email -> eventPublisher.publishEvent(EmailNotificationEvent.builder()
                    .to(email)
                    .subject("[DeVine] 신고 처리 결과 안내 (신고 번호 %d)".formatted(complaint.getId()))
                    .body(content.replace("\n", "<br />"))
                    .build()));
        }
    }

    private boolean isWithdrawn(Member member) {
        return member.getUsed() == MemberStatus.DELETED;
    }

    private Optional<String> findEmail(Member member) {
        return contactRepository.findAllByMember(member).stream()
                .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                .map(Contact::getValue)
                .findFirst();
    }
}
