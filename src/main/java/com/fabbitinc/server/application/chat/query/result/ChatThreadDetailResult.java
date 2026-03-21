package com.fabbitinc.server.application.chat.query.result;

import com.fabbitinc.server.domain.chat.model.ChatThreadStatus;
import java.time.Instant;
import java.util.UUID;

public record ChatThreadDetailResult(
        UUID threadId,
        UUID projectId,
        String contextType,
        UUID contextId,
        String title,
        ChatThreadStatus status,
        Instant lastMessageAt,
        Instant createdAt
) {
}
