package com.fabbitinc.server.application.auth.usecase.result;

public record AcceptInvitationResult(
        AuthUserResult user,
        AuthOrganizationResult organization,
        AuthTokenResult tokens,
        boolean isNewUser
) {
}
