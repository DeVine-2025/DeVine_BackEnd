package com.umc.devine.domain.chat.repository;

import com.umc.devine.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import com.umc.devine.domain.member.entity.Member;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /** 회원 탈퇴용: 이 회원이 참여한 채팅방. 방은 상대방을 위해 남기고 이 회원 쪽만 나간 것으로 바꾼다. */
    @Query("SELECT r FROM ChatRoom r WHERE r.member1 = :member OR r.member2 = :member")
    List<ChatRoom> findAllByParticipant(@Param("member") Member member);

    Optional<ChatRoom> findByMember1IdAndMember2Id(Long member1Id, Long member2Id);

    @Query(value = """
            SELECT cr.chat_room_id, COALESCE(MAX(cm.created_at), cr.created_at) AS last_activity
            FROM chat_room cr
            LEFT JOIN chat_message cm ON cm.chat_room_id = cr.chat_room_id
            WHERE (cr.member1_id = :memberId AND cr.member1_left = false)
               OR (cr.member2_id = :memberId AND cr.member2_left = false)
            GROUP BY cr.chat_room_id
            ORDER BY last_activity DESC
            """, nativeQuery = true)
    List<Object[]> findActiveRoomIdsSortedByActivity(@Param("memberId") Long memberId);

    @Query("""
            SELECT cr FROM ChatRoom cr
            JOIN FETCH cr.member1
            JOIN FETCH cr.member2
            WHERE cr.id IN :roomIds
            """)
    List<ChatRoom> findRoomsWithMembersByIds(@Param("roomIds") List<Long> roomIds);

    @Query("""
            SELECT cr FROM ChatRoom cr
            JOIN FETCH cr.member1
            JOIN FETCH cr.member2
            WHERE cr.id = :roomId
            """)
    Optional<ChatRoom> findByIdWithMembers(@Param("roomId") Long roomId);
}
