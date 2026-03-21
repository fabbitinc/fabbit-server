package com.fabbitinc.server.application.chat.model;

import tools.jackson.databind.JsonNode;

public record ChatUiArtifact(
        String type,
        JsonNode payload
) {

    public static ChatUiArtifact of(String type, JsonNode payload) {
        return new ChatUiArtifact(type, payload);
    }
}
