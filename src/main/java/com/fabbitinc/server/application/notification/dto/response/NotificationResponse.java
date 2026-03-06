package com.fabbitinc.server.application.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabbitinc.server.domain.notification.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record NotificationResponse(
        UUID id,
        NotificationType type,
        UUID actorId,
        MentionPayloadResponse payload,
        Instant readAt,
        Instant createdAt
) {
}
