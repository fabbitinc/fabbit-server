package com.fabbitinc.server.domain.notification.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "ix_notifications_user_unread", columnList = "user_id,read_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends AbstractAuditableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "read_at")
    private Instant readAt;

    public Notification(UUID userId, NotificationType type, UUID actorId, String payload) {
        super(UuidV7Generator.next());
        this.userId = userId;
        this.type = type;
        this.actorId = actorId;
        this.payload = payload;
    }

    public void markRead(Instant readAt) {
        this.readAt = readAt;
    }
}
