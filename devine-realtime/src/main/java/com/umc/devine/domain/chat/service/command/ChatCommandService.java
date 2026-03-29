package com.umc.devine.domain.chat.service.command;

import com.umc.devine.domain.chat.dto.ChatResDTO;

public interface ChatCommandService {

    ChatResDTO.ChatRoomInfo createOrGetRoom(Long memberId, Long targetMemberId);

    void sendMessage(Long memberId, Long roomId, String content);

    ChatResDTO.ReadResult markAsRead(Long memberId, Long roomId);

    void leaveRoom(Long memberId, Long roomId);
}
