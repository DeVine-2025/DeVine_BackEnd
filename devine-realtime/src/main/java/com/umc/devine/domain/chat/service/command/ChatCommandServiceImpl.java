package com.umc.devine.domain.chat.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.chat.converter.ChatConverter;
import com.umc.devine.domain.chat.dto.ChatResDTO;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.exception.ChatException;
import com.umc.devine.domain.chat.exception.code.ChatErrorReason;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.chat.repository.ChatRoomRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.infrastructure.chat.presence.ChatPresenceManager;
import com.umc.devine.infrastructure.redis.ChatEventType;
import com.umc.devine.infrastructure.redis.dto.ChatEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatCommandServiceImpl implements ChatCommandService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final ChatPresenceManager chatPresenceManager;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${redis.channel.chat-prefix}")
    private String chatChannelPrefix;

    @Override
    public ChatResDTO.ChatRoomInfo createOrGetRoom(Long memberId, String targetClerkId) {
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() -> new ChatException(ChatErrorReason.TARGET_MEMBER_NOT_FOUND));
        Member target = memberRepository.findByClerkId(targetClerkId)
                .orElseThrow(() -> new ChatException(ChatErrorReason.TARGET_MEMBER_NOT_FOUND));

        if (memberId.equals(target.getId())) {
            throw new ChatException(ChatErrorReason.CANNOT_CHAT_SELF);
        }

        Long m1Id = Math.min(memberId, target.getId());
        Long m2Id = Math.max(memberId, target.getId());

        ChatRoom room = chatRoomRepository.findByMember1IdAndMember2Id(m1Id, m2Id)
                .map(existing -> {
                    existing.rejoin(memberId);
                    return existing;
                })
                .orElseGet(() -> {
                    try {
                        Member member1 = m1Id.equals(memberId) ? sender : target;
                        Member member2 = m2Id.equals(memberId) ? sender : target;
                        ChatRoom newRoom = ChatRoom.builder()
                                .member1(member1)
                                .member2(member2)
                                .build();
                        return chatRoomRepository.save(newRoom);
                    } catch (DataIntegrityViolationException e) {
                        return chatRoomRepository.findByMember1IdAndMember2Id(m1Id, m2Id)
                                .orElseThrow(() -> new ChatException(ChatErrorReason.CHAT_ROOM_NOT_FOUND));
                    }
                });

        return ChatConverter.toChatRoomInfo(room, memberId);
    }

    @Override
    public void sendMessage(Long memberId, Long roomId, String content) {
        ChatRoom room = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorReason.CHAT_ROOM_NOT_FOUND));

        if (!room.getMember1().getId().equals(memberId) && !room.getMember2().getId().equals(memberId)) {
            throw new ChatException(ChatErrorReason.NOT_CHAT_ROOM_MEMBER);
        }

        if (room.isBothLeft()) {
            throw new ChatException(ChatErrorReason.BOTH_LEFT_ROOM);
        }

        Member sender = room.getMember1().getId().equals(memberId) ? room.getMember1() : room.getMember2();
        Member receiver = room.getOtherMember(memberId);
        Long receiverId = receiver.getId();

        room.rejoin(receiverId);

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .build();

        boolean receiverInRoom = chatPresenceManager.isInRoom(roomId, receiverId);
        if (receiverInRoom) {
            message.markAsRead();
        }

        ChatMessage saved = chatMessageRepository.save(message);

        long unreadRoomCount = receiverInRoom ? 0 :
                chatMessageRepository.countRoomsWithUnreadMessages(receiverId);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publishChatMessage(saved, receiver, sender, receiverInRoom, unreadRoomCount);
                    }
                }
        );
    }

    @Override
    public ChatResDTO.ReadResult markAsRead(Long memberId, Long roomId) {
        ChatRoom room = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorReason.CHAT_ROOM_NOT_FOUND));

        if (!room.getMember1().getId().equals(memberId) && !room.getMember2().getId().equals(memberId)) {
            throw new ChatException(ChatErrorReason.NOT_CHAT_ROOM_MEMBER);
        }

        Member other = room.getOtherMember(memberId);
        Long senderId = other.getId();

        int updated = chatMessageRepository.markAllAsRead(roomId, senderId, LocalDateTime.now());

        long unreadRoomCount = chatMessageRepository.countRoomsWithUnreadMessages(memberId);

        if (updated > 0) {
            Member reader = room.getMember1().getId().equals(memberId) ? room.getMember1() : room.getMember2();
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publishReadEvent(roomId, other, reader);
                        }
                    }
            );
        }

        return ChatResDTO.ReadResult.builder()
                .unreadRoomCount(unreadRoomCount)
                .build();
    }

    @Override
    public void leaveRoom(Long memberId, Long roomId) {
        ChatRoom room = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorReason.CHAT_ROOM_NOT_FOUND));

        if (!room.getMember1().getId().equals(memberId) && !room.getMember2().getId().equals(memberId)) {
            throw new ChatException(ChatErrorReason.NOT_CHAT_ROOM_MEMBER);
        }

        room.leave(memberId);
    }

    private void publishChatMessage(ChatMessage message, Member receiver, Member sender,
                                     boolean receiverInRoom, long unreadRoomCount) {
        try {
            Map<String, Object> data = Map.of(
                    "messageId", message.getId(),
                    "roomId", message.getChatRoom().getId(),
                    "senderClerkId", sender.getClerkId(),
                    "senderNickname", sender.getNickname(),
                    "senderImage", sender.getImage() != null ? sender.getImage() : "",
                    "content", message.getContent(),
                    "isRead", message.getIsRead(),
                    "createdAt", message.getCreatedAt().toString(),
                    "unreadRoomCount", unreadRoomCount
            );

            ChatEventPayload payload = ChatEventPayload.builder()
                    .eventId(String.valueOf(message.getId()))
                    .eventType(ChatEventType.CHAT_MESSAGE)
                    .receiverId(receiver.getId())
                    .receiverClerkId(receiver.getClerkId())
                    .senderClerkId(sender.getClerkId())
                    .data(data)
                    .build();

            String channel = chatChannelPrefix + receiver.getId();
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("채팅 메시지 Redis publish 실패 - messageId: {}", message.getId(), e);
        }
    }

    private void publishReadEvent(Long roomId, Member sender, Member reader) {
        try {
            Map<String, Object> data = Map.of(
                    "roomId", roomId,
                    "readerClerkId", reader.getClerkId()
            );

            ChatEventPayload payload = ChatEventPayload.builder()
                    .eventId(null)
                    .eventType(ChatEventType.CHAT_READ)
                    .receiverId(sender.getId())
                    .receiverClerkId(sender.getClerkId())
                    .senderClerkId(reader.getClerkId())
                    .data(data)
                    .build();

            String channel = chatChannelPrefix + sender.getId();
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("읽음 이벤트 Redis publish 실패 - roomId: {}", roomId, e);
        }
    }
}
