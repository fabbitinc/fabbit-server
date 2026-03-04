package com.fabbitinc.server.application.auth.dto.response;

public record LoginResponse(
        UserResponse user,
        TokenResponse tokens
) {
}
