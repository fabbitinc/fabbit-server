package com.fabbitinc.server.application.chat.query.result;

import com.fabbitinc.server.domain.chat.model.ChatThreadStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatThreadListResult(
        List<Item> items
) {
    public record Item(
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
}
