package com.fabbitinc.server.domain.notification.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    public static final String CODE_NOTIFICATION_USER_REQUIRED = "NOTIFICATION_USER_REQUIRED";
    public static final String CODE_NOTIFICATION_TYPE_REQUIRED = "NOTIFICATION_TYPE_REQUIRED";
    public static final String CODE_NOTIFICATION_ACTOR_REQUIRED = "NOTIFICATION_ACTOR_REQUIRED";
    public static final String CODE_NOTIFICATION_READ_AT_REQUIRED = "NOTIFICATION_READ_AT_REQUIRED";

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", insertable = false, updatable = false)
    private User actor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "read_at")
    private Instant readAt;

    public Notification(UUID userId, NotificationType type, UUID actorId, String payload) {
        super(UuidV7Generator.next());
        this.userId = requireUserId(userId);
        this.type = requireType(type);
        this.actorId = requireActorId(actorId);
        this.payload = normalizePayload(payload);
    }

    public static Notification create(UUID userId, NotificationType type, UUID actorId, String payload) {
        return new Notification(userId, type, actorId, payload);
    }

    public static Notification create(User user, NotificationType type, User actor, String payload) {
        if (user == null) {
            throw new DomainException(CODE_NOTIFICATION_USER_REQUIRED, "알림 수신자 ID는 필수입니다");
        }
        if (actor == null) {
            throw new DomainException(CODE_NOTIFICATION_ACTOR_REQUIRED, "행위자 ID는 필수입니다");
        }
        Notification notification = new Notification(user.getId(), type, actor.getId(), payload);
        notification.user = user;
        notification.actor = actor;
        return notification;
    }

    public void markRead(Instant readAt) {
        this.readAt = requireReadAt(readAt);
    }

    private UUID requireUserId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_NOTIFICATION_USER_REQUIRED, "알림 수신자 ID는 필수입니다");
        }
        return value;
    }

    private NotificationType requireType(NotificationType value) {
        if (value == null) {
            throw new DomainException(CODE_NOTIFICATION_TYPE_REQUIRED, "알림 타입은 필수입니다");
        }
        return value;
    }

    private UUID requireActorId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_NOTIFICATION_ACTOR_REQUIRED, "행위자 ID는 필수입니다");
        }
        return value;
    }

    private Instant requireReadAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_NOTIFICATION_READ_AT_REQUIRED, "읽음 시각은 필수입니다");
        }
        return value;
    }

    private String normalizePayload(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        return value.trim();
    }
}
