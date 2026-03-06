package com.fabbitinc.server.application.activation.service.output;

public record GraphQueryResultOutput(
        String type,
        String key,
        String label,
        String description,
        Long value
) {
}
