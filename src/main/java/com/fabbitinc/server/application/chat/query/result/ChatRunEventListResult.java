package com.fabbitinc.server.application.chat.query.result;

import com.fabbitinc.server.domain.chat.model.ChatRunEventVisibility;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatRunEventListResult(
        List<Item> items
) {
    public record Item(
            UUID eventId,
            UUID runId,
            long sequence,
            String eventType,
            ChatRunEventVisibility visibility,
            String payload,
            Instant createdAt
    ) {
    }
}
