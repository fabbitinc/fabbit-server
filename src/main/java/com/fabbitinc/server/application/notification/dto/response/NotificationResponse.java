package com.fabbitinc.server.application.notification.dto.response;

import com.fabbitinc.server.domain.notification.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UUID actorId,
        MentionPayloadResponse payload,
        Instant readAt,
        Instant createdAt
) {
}
