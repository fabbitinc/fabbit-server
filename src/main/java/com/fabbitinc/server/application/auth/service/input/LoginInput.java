package com.fabbitinc.server.application.auth.service.input;

public record LoginInput(
        String email,
        String password
) {
}
