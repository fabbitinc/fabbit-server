package com.fabbitinc.server.application.auth.dto.response;

public record ScopedLoginResponse(
        UserResponse user,
        String scopedToken
) {
}
