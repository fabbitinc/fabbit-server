package com.fabbitinc.server.application.auth.dto.response;

public record CheckEmailResponse(
        boolean available,
        String message
) {
}
