package com.fabbitinc.server.application.auth.dto.response;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName
) {
}
