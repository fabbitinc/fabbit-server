package com.fabbitinc.server.domain.notification.repository;

import com.fabbitinc.server.domain.notification.model.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying
    @Query("""
            update Notification n
            set n.readAt = ?3
            where n.userId = ?1
              and n.id = ?2
              and n.readAt is null
            """)
    int markAsRead(UUID userId, UUID notificationId, Instant readAt);

    @Modifying
    @Query("""
            update Notification n
            set n.readAt = ?2
            where n.userId = ?1
              and n.readAt is null
            """)
    int markAllAsRead(UUID userId, Instant readAt);
}
