package com.fabbitinc.server.application.activation.usecase.result;

public record QueryGraphItemResult(
        String type,
        String key,
        String label,
        String description,
        Long value
) {
}
