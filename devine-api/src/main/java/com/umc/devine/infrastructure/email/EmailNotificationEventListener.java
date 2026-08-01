package com.umc.devine.infrastructure.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationEventListener {

    private final EmailSender emailSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailNotification(EmailNotificationEvent event) {
        try {
            emailSender.send(event.getTo(), event.getSubject(), event.getBody());
        } catch (Exception e) {
            log.warn("이메일 발송 실패 - to: {}, subject: {}", event.getTo(), event.getSubject(), e);
        }
    }
}
