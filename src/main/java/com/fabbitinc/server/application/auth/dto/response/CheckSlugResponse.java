package com.fabbitinc.server.application.auth.dto.response;

public record CheckSlugResponse(
        boolean available,
        String message,
        String suggestion
) {
    public static CheckSlugResponse asAvailable() {
        return new CheckSlugResponse(true, null, null);
    }
}
