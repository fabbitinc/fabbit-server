package com.fabbitinc.server.application.chat.model;

import com.fabbitinc.server.domain.chat.model.ChatActionRequest;

public record ChatPendingAction(
        ChatActionRequest actionRequest,
        ChatUiArtifact uiArtifact
) {
}
