package com.umc.devine.domain.notification.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.notification.converter.NotificationConverter;
import com.umc.devine.domain.notification.dto.NotificationResDTO;
import com.umc.devine.domain.notification.exception.code.NotificationSuccessCode;
import com.umc.devine.domain.notification.service.command.NotificationCommandService;
import com.umc.devine.domain.notification.service.query.NotificationQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationControllerDocs {

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;

    @Override
    @GetMapping
    public ApiResponse<NotificationResDTO.NotificationList> getNotifications(
            @CurrentMember Member member,
            @RequestParam(defaultValue = "false") Boolean unreadOnly,
            Pageable pageable
    ) {
        return ApiResponse.onSuccess(
                NotificationSuccessCode.FETCH_SUCCESS,
                notificationQueryService.getNotifications(member.getId(), unreadOnly, pageable)
        );
    }

    @Override
    @GetMapping("/unread-count")
    public ApiResponse<NotificationResDTO.UnreadCount> getUnreadCount(
            @CurrentMember Member member
    ) {
        return ApiResponse.onSuccess(
                NotificationSuccessCode.FETCH_SUCCESS,
                notificationQueryService.getUnreadCount(member.getId())
        );
    }

    @Override
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @CurrentMember Member member,
            @PathVariable("notificationId") Long notificationId
    ) {
        notificationCommandService.markAsRead(notificationId, member.getId());

        return ApiResponse.onSuccess(NotificationSuccessCode.MARK_READ_SUCCESS, null);
    }

    @Override
    @PatchMapping("/read-all")
    public ApiResponse<NotificationResDTO.MarkAllReadResult> markAllAsRead(
            @CurrentMember Member member
    ) {
        int count = notificationCommandService.markAllAsRead(member.getId());

        return ApiResponse.onSuccess(
                NotificationSuccessCode.MARK_ALL_READ_SUCCESS,
                NotificationConverter.toMarkAllReadResult(count)
        );
    }
}
