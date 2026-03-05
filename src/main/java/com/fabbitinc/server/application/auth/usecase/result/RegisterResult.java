package com.fabbitinc.server.application.auth.usecase.result;

public record RegisterResult(
        AuthUserResult user,
        AuthOrganizationResult organization,
        AuthTokenResult tokens
) {
}
