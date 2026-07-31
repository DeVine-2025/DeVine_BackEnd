package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 내가 받은 알림은 삭제하고, 내가 보낸 알림은 상대방 알림함에 남기되 발신자 참조만 끊는다. */
@Component
@Order(110)
@RequiredArgsConstructor
public class NotificationHardDeleteHandler implements MemberHardDeleteHandler {

    private final NotificationRepository notificationRepository;

    @Override
    public void handle(Member member) {
        notificationRepository.bulkDeleteByReceiver(member);
        notificationRepository.bulkNullifySender(member);
    }
}
