package com.fabbitinc.server.application.chat.query.condition;

import java.util.UUID;

public record ChatMessageListCondition(
        UUID threadId
) {
}
