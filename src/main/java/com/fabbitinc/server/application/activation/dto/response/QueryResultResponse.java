package com.fabbitinc.server.application.activation.dto.response;

public record QueryResultResponse(
        String type,
        String key,
        String label,
        String description,
        Long value
) {
}
