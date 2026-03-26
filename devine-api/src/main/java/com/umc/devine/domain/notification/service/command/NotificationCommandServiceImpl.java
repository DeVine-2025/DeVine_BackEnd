package com.umc.devine.domain.notification.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.notification.converter.NotificationConverter;
import com.umc.devine.domain.notification.dto.NotificationResDTO;
import com.umc.devine.domain.notification.entity.Notification;
import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.exception.NotificationException;
import com.umc.devine.domain.notification.exception.code.NotificationErrorReason;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import com.umc.devine.infrastructure.redis.RedisEventConstants;
import com.umc.devine.infrastructure.redis.dto.SseEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${redis.channel.notification-prefix}")
    private String notificationChannelPrefix;

    @Override
    public Notification create(
            NotificationType type,
            Long receiverId,
            Long senderId,
            String content,
            Long referenceId
    ) {
        Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() -> new NotificationException(NotificationErrorReason.RECEIVER_NOT_FOUND));

        Member sender = senderId != null
                ? memberRepository.findById(senderId).orElse(null)
                : null;

        Notification notification = NotificationConverter.toNotification(
                type, receiver, sender, content, referenceId
        );

        Notification saved = notificationRepository.save(notification);
        log.info("알림 생성 - type: {}, receiverId: {}, referenceId: {}",
                type, receiverId, referenceId);

        // Redis Pub/Sub으로 실시간 전송 신호 발행 (트랜잭션 커밋 후)
        publishNotificationEvent(saved);

        return saved;
    }

    private void publishNotificationEvent(Notification notification) {
        NotificationResDTO.NotificationDetail detail =
                NotificationConverter.toDetail(notification);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            SseEventPayload payload = SseEventPayload.builder()
                                    .eventId(String.valueOf(notification.getId()))
                                    .eventType(RedisEventConstants.NOTIFICATION)
                                    .receiverId(notification.getReceiver().getId())
                                    .data(detail)
                                    .build();
                            String channel = notificationChannelPrefix + notification.getReceiver().getId();
                            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(payload));
                        } catch (Exception e) {
                            log.error("Redis publish 실패", e);
                        }
                    }
                }
        );
    }

    @Override
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorReason.NOTIFICATION_NOT_FOUND));

        // IDOR 방지: 본인 알림만 읽음 처리 가능
        if (!notification.getReceiver().getId().equals(memberId)) {
            throw new NotificationException(NotificationErrorReason.FORBIDDEN);
        }

        notification.markAsRead();
    }

    @Override
    public int markAllAsRead(Long memberId) {
        int count = notificationRepository.markAllAsRead(memberId, LocalDateTime.now());
        log.info("전체 읽음 처리 - memberId: {}, count: {}", memberId, count);
        return count;
    }
}
