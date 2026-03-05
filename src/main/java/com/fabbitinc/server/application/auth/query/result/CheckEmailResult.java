package com.fabbitinc.server.application.auth.query.result;

public record CheckEmailResult(
        boolean available,
        String message
) {
}
