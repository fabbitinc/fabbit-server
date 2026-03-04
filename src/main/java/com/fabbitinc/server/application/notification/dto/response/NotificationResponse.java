package com.fabbitinc.server.application.notification.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        UUID actorId,
        MentionPayloadResponse payload,
        Instant readAt,
        Instant createdAt
) {
}
