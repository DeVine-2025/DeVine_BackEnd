package com.umc.devine.domain.notification.event;

import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.service.command.NotificationCommandService;
import com.umc.devine.domain.report.event.ReportNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 리포트 도메인 이벤트를 수신하여 알림을 생성하는 리스너
 * 트랜잭션 커밋 후 비동기로 실행되어 리포트 트랜잭션에 영향을 주지 않음
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportNotificationEventListener {

    private final NotificationCommandService notificationCommandService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportNotification(ReportNotificationEvent event) {
        NotificationType type = event.isSuccess()
                ? NotificationType.REPORT_COMPLETED
                : NotificationType.REPORT_FAILED;

        try {
            notificationCommandService.create(
                    type,
                    event.getReceiverId(),
                    null,
                    event.getGitUrl(),
                    event.getReportId()
            );
            log.debug("리포트 알림 생성 완료 - type: {}, receiverId: {}, reportId: {}",
                    type, event.getReceiverId(), event.getReportId());
        } catch (Exception e) {
            log.warn("리포트 알림 생성 실패 - type: {}, receiverId: {}, reportId: {}",
                    type, event.getReceiverId(), event.getReportId(), e);
        }
    }
}
