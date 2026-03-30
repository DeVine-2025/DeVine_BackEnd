package com.umc.devine.domain.chat.controller;

import com.umc.devine.domain.chat.dto.ChatReqDTO;
import com.umc.devine.domain.chat.dto.ChatResDTO;
import com.umc.devine.domain.chat.exception.code.ChatSuccessCode;
import com.umc.devine.domain.chat.service.command.ChatCommandService;
import com.umc.devine.domain.chat.service.query.ChatQueryService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatCommandService chatCommandService;
    private final ChatQueryService chatQueryService;

    @PostMapping("/rooms")
    public ApiResponse<ChatResDTO.ChatRoomInfo> createOrGetRoom(
            @CurrentMember Member member,
            @Valid @RequestBody ChatReqDTO.CreateRoomReq request
    ) {
        return ApiResponse.onSuccess(
                ChatSuccessCode.ROOM_CREATED,
                chatCommandService.createOrGetRoom(member.getId(), request.targetClerkId())
        );
    }

    @GetMapping("/rooms")
    public ApiResponse<ChatResDTO.ChatRoomList> getRoomList(
            @CurrentMember Member member
    ) {
        return ApiResponse.onSuccess(
                ChatSuccessCode.ROOM_LIST_FETCHED,
                chatQueryService.getRoomList(member.getId())
        );
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatResDTO.MessageList> getMessages(
            @CurrentMember Member member,
            @PathVariable("roomId") Long roomId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.onSuccess(
                ChatSuccessCode.MESSAGES_FETCHED,
                chatQueryService.getMessages(member.getId(), roomId, pageable)
        );
    }

    @PatchMapping("/rooms/{roomId}/read")
    public ApiResponse<ChatResDTO.ReadResult> markAsRead(
            @CurrentMember Member member,
            @PathVariable("roomId") Long roomId
    ) {
        return ApiResponse.onSuccess(
                ChatSuccessCode.READ_SUCCESS,
                chatCommandService.markAsRead(member.getId(), roomId)
        );
    }

    @DeleteMapping("/rooms/{roomId}")
    public ApiResponse<Void> leaveRoom(
            @CurrentMember Member member,
            @PathVariable("roomId") Long roomId
    ) {
        chatCommandService.leaveRoom(member.getId(), roomId);
        return ApiResponse.onSuccess(ChatSuccessCode.LEAVE_SUCCESS, null);
    }

    @GetMapping("/unread-count")
    public ApiResponse<ChatResDTO.UnreadRoomCount> getUnreadCount(
            @CurrentMember Member member
    ) {
        return ApiResponse.onSuccess(
                ChatSuccessCode.UNREAD_COUNT_FETCHED,
                chatQueryService.getTotalUnreadCount(member.getId())
        );
    }
}
