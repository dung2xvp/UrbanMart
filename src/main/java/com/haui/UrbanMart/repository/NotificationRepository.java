package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.Notification;
import com.haui.UrbanMart.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.user.id = :userId
              AND (:type IS NULL OR notification.type = :type)
              AND (:isRead IS NULL OR notification.isRead = :isRead)
            """)
    Page<Notification> findForUser(
            @Param("userId") UUID userId,
            @Param("type") NotificationType type,
            @Param("isRead") Boolean isRead,
            Pageable pageable
    );

    Optional<Notification> findByIdAndUser_Id(UUID id, UUID userId);

    @Query("""
            SELECT COUNT(notification)
            FROM Notification notification
            WHERE notification.user.id = :userId
              AND notification.isRead = false
            """)
    long countUnreadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("""
            UPDATE Notification notification
            SET notification.isRead = true
            WHERE notification.user.id = :userId
              AND notification.isRead = false
            """)
    int markAllRead(@Param("userId") UUID userId);
}
