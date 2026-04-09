package com.umc.devine.domain.chat.service.query;

import com.umc.devine.domain.chat.dto.ChatResDTO;
import org.springframework.data.domain.Pageable;

public interface ChatQueryService {

    ChatResDTO.ChatRoomList getRoomList(Long memberId);

    ChatResDTO.MessageList getMessages(Long memberId, Long roomId, Pageable pageable);

    ChatResDTO.UnreadRoomCount getTotalUnreadCount(Long memberId);
}
