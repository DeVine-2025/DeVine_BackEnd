package com.umc.devine.domain.project.helper;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.service.command.NotificationCommandService;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 매칭 관련한 알림 메시지 생성 및 전송 헬퍼
@Component
@RequiredArgsConstructor
public class MatchingNotificationHelper {

    private final NotificationCommandService notificationCommandService;

    public void notifyApply(Matching matching) {
        Member applicant = matching.getMember();
        Project project = matching.getProject();

        send(
                NotificationType.MATCHING_APPLIED,
                project.getMember().getId(),
                applicant.getId(),
                "%s님이 '%s' 프로젝트에 지원했습니다.".formatted(applicant.getNickname(), project.getName()),
                matching.getId()
        );
    }

    public void notifyPropose(Member pm, Matching matching) {
        Member targetMember = matching.getMember();
        Project project = matching.getProject();

        send(
                NotificationType.MATCHING_PROPOSED,
                targetMember.getId(),
                pm.getId(),
                "%s님이 '%s' 프로젝트 참여를 제안했습니다.".formatted(pm.getNickname(), project.getName()),
                matching.getId()
        );
    }

    public void notifyApplicationDecision(Matching matching, MatchingDecision decision) {
        Project project = matching.getProject();
        String actionText = decision == MatchingDecision.ACCEPT ? "수락" : "거절";

        send(
                getNotificationType(decision),
                matching.getMember().getId(),
                project.getMember().getId(),
                "'%s' 프로젝트 지원이 %s되었습니다.".formatted(project.getName(), actionText),
                matching.getId()
        );
    }

    public void notifyProposalDecision(Matching matching, MatchingDecision decision) {
        Member developer = matching.getMember();
        Project project = matching.getProject();
        String actionText = decision == MatchingDecision.ACCEPT ? "수락" : "거절";

        send(
                getNotificationType(decision),
                project.getMember().getId(),
                developer.getId(),
                "%s님이 '%s' 프로젝트 제안을 %s했습니다.".formatted(developer.getNickname(), project.getName(), actionText),
                matching.getId()
        );
    }

    private NotificationType getNotificationType(MatchingDecision decision) {
        return decision == MatchingDecision.ACCEPT
                ? NotificationType.MATCHING_ACCEPTED
                : NotificationType.MATCHING_REJECTED;
    }

    private void send(NotificationType type, Long receiverId, Long senderId, String content, Long matchingId) {
        notificationCommandService.create(type, receiverId, senderId, content, matchingId);
    }
}
