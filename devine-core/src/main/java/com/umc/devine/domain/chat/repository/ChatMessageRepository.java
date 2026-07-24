package com.umc.devine.domain.chat.repository;

import com.umc.devine.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            SELECT cm FROM ChatMessage cm
            WHERE cm.chatRoom.id = :chatRoomId
            ORDER BY cm.createdAt DESC
            """)
    Slice<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable
    );

    @Query("""
            SELECT cm FROM ChatMessage cm
            WHERE cm.chatRoom.id = :chatRoomId
              AND cm.createdAt > :leftAt
            ORDER BY cm.createdAt DESC
            """)
    Slice<ChatMessage> findByChatRoomIdAndCreatedAtAfterOrderByCreatedAtDesc(
            @Param("chatRoomId") Long chatRoomId,
            @Param("leftAt") LocalDateTime leftAt,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ChatMessage cm
            SET cm.isRead = true, cm.readAt = :now
            WHERE cm.chatRoom.id = :chatRoomId
              AND cm.sender.id = :senderId
              AND cm.isRead = false
            """)
    int markAllAsRead(
            @Param("chatRoomId") Long chatRoomId,
            @Param("senderId") Long senderId,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT COUNT(DISTINCT cm.chatRoom.id)
            FROM ChatMessage cm
            JOIN cm.chatRoom cr
            WHERE cm.sender.id != :memberId
              AND cm.isRead = false
              AND (
                (cr.member1.id = :memberId AND cr.member1Left = false
                    AND (cr.member1LeftAt IS NULL OR cm.createdAt > cr.member1LeftAt))
                OR (cr.member2.id = :memberId AND cr.member2Left = false
                    AND (cr.member2LeftAt IS NULL OR cm.createdAt > cr.member2LeftAt))
              )
            """)
    long countRoomsWithUnreadMessages(@Param("memberId") Long memberId);

    @Query("""
            SELECT cm.chatRoom.id, COUNT(cm)
            FROM ChatMessage cm
            JOIN cm.chatRoom cr
            WHERE cr.id IN :roomIds
              AND cm.sender.id != :memberId
              AND cm.isRead = false
              AND (
                (cr.member1.id = :memberId
                    AND (cr.member1LeftAt IS NULL OR cm.createdAt > cr.member1LeftAt))
                OR (cr.member2.id = :memberId
                    AND (cr.member2LeftAt IS NULL OR cm.createdAt > cr.member2LeftAt))
              )
            GROUP BY cm.chatRoom.id
            """)
    List<Object[]> countUnreadPerRoom(
            @Param("roomIds") List<Long> roomIds,
            @Param("memberId") Long memberId
    );

    Optional<ChatMessage> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    @Query("""
            SELECT cm FROM ChatMessage cm
            JOIN FETCH cm.sender
            WHERE cm.id IN (
                SELECT MAX(cm2.id) FROM ChatMessage cm2
                JOIN cm2.chatRoom cr2
                WHERE cr2.id IN :roomIds
                  AND (
                    (cr2.member1.id = :memberId
                        AND (cr2.member1LeftAt IS NULL OR cm2.createdAt > cr2.member1LeftAt))
                    OR (cr2.member2.id = :memberId
                        AND (cr2.member2LeftAt IS NULL OR cm2.createdAt > cr2.member2LeftAt))
                  )
                GROUP BY cm2.chatRoom.id
            )
            """)
    List<ChatMessage> findLastMessagesByRoomIds(
            @Param("roomIds") List<Long> roomIds,
            @Param("memberId") Long memberId
    );
}
