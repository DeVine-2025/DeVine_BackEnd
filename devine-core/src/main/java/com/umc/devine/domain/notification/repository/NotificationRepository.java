package com.umc.devine.domain.notification.repository;

import com.umc.devine.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.receiver.id = :receiverId AND n.isRead = false ORDER BY n.createdAt DESC")
    @EntityGraph(attributePaths = { "sender" })
    Slice<Notification> findUnreadByReceiverId(
            @Param("receiverId") Long receiverId,
            Pageable pageable
    );

    @Query("SELECT n FROM Notification n WHERE n.receiver.id = :receiverId ORDER BY n.createdAt DESC")
    @EntityGraph(attributePaths = { "sender" })
    Slice<Notification> findAllByReceiverId(
            @Param("receiverId") Long receiverId,
            Pageable pageable
    );

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
            "WHERE n.receiver.id = :receiverId AND n.isRead = false")
    int markAllAsRead(
            @Param("receiverId") Long receiverId,
            @Param("now") LocalDateTime now
    );

    /**
     * 재연결 시 놓친 알림 조회
     * @param receiverId 수신자 ID
     * @param lastEventId 마지막으로 수신한 알림 ID
     * @param pageable 페이징 설정 (최대 조회 개수 제한용)
     * @return 놓친 알림 목록 (lastEventId 이후의 알림, ID 오름차순)
     */
    @Query("SELECT n FROM Notification n " +
            "LEFT JOIN FETCH n.sender " +
            "WHERE n.receiver.id = :receiverId AND n.id > :lastEventId " +
            "ORDER BY n.id ASC")
    List<Notification> findMissedNotifications(
            @Param("receiverId") Long receiverId,
            @Param("lastEventId") Long lastEventId,
            Pageable pageable
    );
}
