package com.fabbitinc.server.application.auth.query.result;

public record CheckSlugResult(
        boolean available,
        String message,
        String suggestion
) {
    public static CheckSlugResult asAvailable() {
        return new CheckSlugResult(true, null, null);
    }
}
