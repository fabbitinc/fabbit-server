package com.fabbitinc.server.application.chat.query.result;

import com.fabbitinc.server.domain.chat.model.ChatMessageRole;
import com.fabbitinc.server.domain.chat.model.ChatMessageStatus;
import com.fabbitinc.server.domain.chat.model.ChatMessageType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessageListResult(
        List<Item> items
) {
    public record Item(
            UUID messageId,
            UUID runId,
            ChatMessageRole role,
            ChatMessageType messageType,
            ChatMessageStatus status,
            long sequence,
            String content,
            Instant createdAt
    ) {
    }
}
