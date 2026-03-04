package com.fabbitinc.server.application.auth.dto.response;

public record RegisterResponse(
        UserResponse user,
        OrganizationResponse organization,
        TokenResponse tokens
) {
}
