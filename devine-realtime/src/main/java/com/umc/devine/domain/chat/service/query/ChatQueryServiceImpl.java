package com.umc.devine.domain.chat.service.query;

import com.umc.devine.domain.chat.converter.ChatConverter;
import com.umc.devine.domain.chat.dto.ChatResDTO;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.exception.ChatException;
import com.umc.devine.domain.chat.exception.code.ChatErrorReason;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryServiceImpl implements ChatQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public ChatResDTO.ChatRoomList getRoomList(Long memberId) {
        List<Object[]> activeRooms = chatRoomRepository.findActiveRoomIdsSortedByActivity(memberId);

        if (activeRooms.isEmpty()) {
            return ChatResDTO.ChatRoomList.builder().rooms(List.of()).build();
        }

        List<Long> roomIds = activeRooms.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();

        Map<Long, LocalDateTime> lastActivityMap = activeRooms.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> row[1] instanceof Timestamp ts ? ts.toLocalDateTime() : (LocalDateTime) row[1]
                ));

        Map<Long, ChatRoom> roomMap = chatRoomRepository.findRoomsWithMembersByIds(roomIds).stream()
                .collect(Collectors.toMap(ChatRoom::getId, Function.identity()));

        Map<Long, Long> unreadMap = new HashMap<>();
        chatMessageRepository.countUnreadPerRoom(roomIds, memberId)
                .forEach(row -> unreadMap.put((Long) row[0], (Long) row[1]));

        Map<Long, ChatMessage> lastMsgMap = chatMessageRepository.findLastMessagesByRoomIds(roomIds).stream()
                .collect(Collectors.toMap(cm -> cm.getChatRoom().getId(), Function.identity()));

        List<ChatResDTO.ChatRoomDetail> rooms = roomIds.stream()
                .map(roomId -> {
                    ChatRoom room = roomMap.get(roomId);
                    if (room == null) return null;
                    ChatMessage lastMsg = lastMsgMap.get(roomId);
                    return ChatConverter.toChatRoomDetail(
                            room, memberId,
                            lastMsg != null ? lastMsg.getContent() : null,
                            lastActivityMap.get(roomId),
                            unreadMap.getOrDefault(roomId, 0L)
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        return ChatResDTO.ChatRoomList.builder().rooms(rooms).build();
    }

    @Override
    public ChatResDTO.MessageList getMessages(Long memberId, Long roomId, Pageable pageable) {
        ChatRoom room = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorReason.CHAT_ROOM_NOT_FOUND));

        if (!room.getMember1().getId().equals(memberId) && !room.getMember2().getId().equals(memberId)) {
            throw new ChatException(ChatErrorReason.NOT_CHAT_ROOM_MEMBER);
        }

        LocalDateTime leftAt = room.getLeftAt(memberId);

        Slice<ChatMessage> messages;
        if (leftAt == null) {
            messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId, pageable);
        } else {
            messages = chatMessageRepository.findByChatRoomIdAndCreatedAtAfterOrderByCreatedAtDesc(
                    roomId, leftAt, pageable);
        }

        return ChatConverter.toMessageList(messages, pageable.getPageNumber());
    }

    @Override
    public ChatResDTO.UnreadRoomCount getTotalUnreadCount(Long memberId) {
        long count = chatMessageRepository.countRoomsWithUnreadMessages(memberId);
        return ChatResDTO.UnreadRoomCount.builder().unreadRoomCount(count).build();
    }
}
