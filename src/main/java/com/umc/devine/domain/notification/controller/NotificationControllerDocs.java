package com.umc.devine.domain.notification.controller;

import com.umc.devine.domain.notification.dto.NotificationResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Notification", description = "알림 관련 API")
public interface NotificationControllerDocs {

    @Operation(summary = "알림 목록 조회 API", description = "내 알림 목록을 조회하는 API입니다. unreadOnly=true면 읽지 않은 알림만 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
    })
    ApiResponse<NotificationResDTO.NotificationList> getNotifications(
            @Parameter(description = "읽지 않은 알림만 조회") Boolean unreadOnly,
            @ParameterObject @PageableDefault(size = 20, page = 0) Pageable pageable
    );

    @Operation(summary = "읽지 않은 알림 개수 조회 API", description = "읽지 않은 알림 개수를 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
    })
    ApiResponse<NotificationResDTO.UnreadCount> getUnreadCount();

    @Operation(summary = "알림 읽음 처리 API", description = "특정 알림을 읽음 처리하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "해당 알림에 대한 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없습니다.")
    })
    ApiResponse<Void> markAsRead(
            @Parameter(description = "알림 ID") Long notificationId
    );

    @Operation(summary = "전체 읽음 처리 API", description = "모든 알림을 읽음 처리하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
    })
    ApiResponse<NotificationResDTO.MarkAllReadResult> markAllAsRead();
}
