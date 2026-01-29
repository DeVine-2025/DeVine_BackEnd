package com.umc.devine.domain.notification.controller;

import com.umc.devine.domain.notification.converter.NotificationConverter;
import com.umc.devine.domain.notification.dto.NotificationResDTO;
import com.umc.devine.domain.notification.exception.code.NotificationSuccessCode;
import com.umc.devine.domain.notification.service.command.NotificationCommandService;
import com.umc.devine.domain.notification.service.query.NotificationQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
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
            @RequestParam(defaultValue = "false") Boolean unreadOnly,
            Pageable pageable
    ) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(
                NotificationSuccessCode.FETCH_SUCCESS,
                notificationQueryService.getNotifications(memberId, unreadOnly, pageable)
        );
    }

    @Override
    @GetMapping("/unread-count")
    public ApiResponse<NotificationResDTO.UnreadCount> getUnreadCount() {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(
                NotificationSuccessCode.FETCH_SUCCESS,
                notificationQueryService.getUnreadCount(memberId)
        );
    }

    @Override
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable("notificationId") Long notificationId
    ) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        notificationCommandService.markAsRead(notificationId, memberId);

        return ApiResponse.onSuccess(NotificationSuccessCode.MARK_READ_SUCCESS, null);
    }

    @Override
    @PatchMapping("/read-all")
    public ApiResponse<NotificationResDTO.MarkAllReadResult> markAllAsRead() {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        int count = notificationCommandService.markAllAsRead(memberId);

        return ApiResponse.onSuccess(
                NotificationSuccessCode.MARK_ALL_READ_SUCCESS,
                NotificationConverter.toMarkAllReadResult(count)
        );
    }
}
