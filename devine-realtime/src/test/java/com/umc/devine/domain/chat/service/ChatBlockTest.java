package com.umc.devine.domain.chat.service;

import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.exception.ChatException;
import com.umc.devine.domain.chat.repository.ChatRoomRepository;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.chat.service.command.ChatCommandServiceImpl;
import com.umc.devine.domain.chat.service.query.ChatQueryServiceImpl;
import com.umc.devine.domain.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.MemberBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatBlockTest {
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock MemberRepository memberRepository;
    @Mock MemberBlockRepository blockRepository;
    @Mock ComplaintRepository complaintRepository;
    @InjectMocks ChatCommandServiceImpl commands;
    @InjectMocks ChatQueryServiceImpl queries;

    Member a = Member.builder().id(1L).nickname("a").used(com.umc.devine.domain.member.enums.MemberStatus.ACTIVE).mainType(MemberMainType.PM).build();
    Member b = Member.builder().id(2L).nickname("b").used(com.umc.devine.domain.member.enums.MemberStatus.ACTIVE).mainType(MemberMainType.DEVELOPER).build();
    ChatRoom room = ChatRoom.builder().id(3L).member1(a).member2(b).build();

    @Test
    void preventsCreatingOrRejoiningRoomWhenBlocked() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(a));
        when(memberRepository.findByClerkId("b")).thenReturn(Optional.of(b));
        when(blockRepository.existsBetween(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> commands.createOrGetRoom(1L, "b")).isInstanceOf(ChatException.class);
        verifyNoInteractions(chatRoomRepository);
    }

    @Test
    void preventsSendingButPreservesMessageHistory() {
        when(chatRoomRepository.findByIdWithMembers(3L)).thenReturn(Optional.of(room));
        when(blockRepository.existsBetween(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> commands.sendMessage(1L, 3L, "new")).isInstanceOf(ChatException.class);
        verify(chatMessageRepository, never()).save(any());

        var pageable = PageRequest.of(0, 10);
        var oldMessage = ChatMessage.builder().id(4L).chatRoom(room).sender(a).content("old").build();
        when(chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(3L, pageable))
                .thenReturn(new SliceImpl<>(List.of(oldMessage)));
        var result = queries.getMessages(1L, 3L, pageable);
        assertThat(result.messages()).extracting("content").containsExactly("old");
        assertThat(result.canSend()).isFalse();
    }
}
